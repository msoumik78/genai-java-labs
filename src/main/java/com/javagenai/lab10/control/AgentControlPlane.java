package com.javagenai.lab10.control;

import com.javagenai.lab10.agent.Agent;
import com.javagenai.lab10.task.Task;
import com.javagenai.lab10.task.TaskEvent;
import com.javagenai.lab10.task.TaskState;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Finds an agent for each capability on the route and runs it.
 * A peer that refuses or throws is skipped. The next peer for that capability is tried.
 * Task state changes only through the allowed transitions.
 */
public final class AgentControlPlane implements AutoCloseable {

    private final ConcurrentHashMap<String, List<Agent>> registry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Run> runs = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<TaskEvent> events = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<TaskEvent>> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService workers = Executors.newFixedThreadPool(4, Thread.ofPlatform().name("agent-", 0).factory());

    public void register(Agent agent) {
        for (String capability : agent.card().capabilities()) {
            registry.computeIfAbsent(capability, key -> new CopyOnWriteArrayList<>()).add(agent);
        }
    }

    public void onEvent(Consumer<TaskEvent> listener) {
        listeners.add(listener);
    }

    public List<TaskEvent> events() {
        return List.copyOf(events);
    }

    public TaskState state(String taskId) {
        return run(taskId).state.get();
    }

    public CompletableFuture<String> submit(Task task) {
        Run run = new Run(task);
        if (runs.putIfAbsent(task.id(), run) != null) {
            throw new IllegalStateException("task already submitted: " + task.id());
        }
        publish(task.id(), TaskState.PENDING, "accepted");
        CompletableFuture<String> result = CompletableFuture.supplyAsync(() -> execute(run), workers);
        run.future.set(result);
        result.whenComplete((value, error) -> finish(run, error));
        return result;
    }

    public boolean cancel(String taskId) {
        Run run = run(taskId);
        run.cancelled.set(true);
        boolean changed = run.transition(TaskState.PENDING, TaskState.CANCELLED, "cancelled")
                || run.transition(TaskState.RUNNING, TaskState.CANCELLED, "cancelled");
        CompletableFuture<?> future = run.future.get();
        if (future != null) {
            future.cancel(true);
        }
        return changed;
    }

    @Override
    public void close() {
        workers.shutdownNow();
    }

    private String execute(Run run) {
        if (!run.transition(TaskState.PENDING, TaskState.RUNNING, "running")) {
            throw new CancellationException();
        }
        String input = run.task.payload();
        for (String capability : run.task.route()) {
            if (run.cancelled.get()) {
                throw new CancellationException();
            }
            input = call(run, capability, input);
        }
        if (!run.transition(TaskState.RUNNING, TaskState.COMPLETED, "completed")) {
            throw new CancellationException();
        }
        return input;
    }

    private String call(Run run, String capability, String input) {
        List<Agent> peers = registry.getOrDefault(capability, List.of());
        if (peers.isEmpty()) {
            throw new IllegalStateException("no agent for " + capability);
        }
        RuntimeException last = new IllegalStateException("no peer for " + capability);
        for (Agent peer : peers) {
            if (run.cancelled.get()) {
                throw new CancellationException();
            }
            if (!peer.accept(capability)) {
                continue;
            }
            try {
                return peer.execute(input);
            } catch (RuntimeException error) {
                last = error;
            }
        }
        throw last;
    }

    private void finish(Run run, Throwable error) {
        if (error == null) {
            return;
        }
        Throwable cause = error.getCause() == null ? error : error.getCause();
        if (run.cancelled.get() || cause instanceof CancellationException) {
            run.transition(TaskState.PENDING, TaskState.CANCELLED, "cancelled");
            run.transition(TaskState.RUNNING, TaskState.CANCELLED, "cancelled");
            return;
        }
        String detail = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        run.transition(TaskState.RUNNING, TaskState.FAILED, detail);
    }

    private Run run(String taskId) {
        Run run = runs.get(taskId);
        if (run == null) {
            throw new NoSuchElementException(taskId);
        }
        return run;
    }

    private void publish(String taskId, TaskState state, String detail) {
        TaskEvent event = new TaskEvent(taskId, state, detail);
        events.add(event);
        for (Consumer<TaskEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    private final class Run {
        final Task task;
        final AtomicReference<TaskState> state = new AtomicReference<>(TaskState.PENDING);
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicReference<CompletableFuture<?>> future = new AtomicReference<>();

        Run(Task task) {
            this.task = task;
        }

        boolean transition(TaskState from, TaskState to, String detail) {
            if (!from.canTransitionTo(to) || !state.compareAndSet(from, to)) {
                return false;
            }
            publish(task.id(), to, detail);
            return true;
        }
    }
}
