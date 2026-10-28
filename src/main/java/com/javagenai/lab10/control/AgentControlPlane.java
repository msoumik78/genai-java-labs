package com.javagenai.lab10.control;

import com.javagenai.lab10.agent.Agent;
import com.javagenai.lab10.task.Task;
import com.javagenai.lab10.task.TaskState;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Finds an agent for each capability on the route and runs it.
 * A failed agent is skipped. The next agent registered for that capability is tried.
 */
public final class AgentControlPlane implements AutoCloseable {

    private final ConcurrentHashMap<String, List<Agent>> registry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicReference<TaskState>> states = new ConcurrentHashMap<>();
    private final ExecutorService workers = Executors.newFixedThreadPool(4, Thread.ofPlatform().name("agent-", 0).factory());

    public void register(Agent agent) {
        for (String capability : agent.card().capabilities()) {
            registry.computeIfAbsent(capability, key -> new CopyOnWriteArrayList<>()).add(agent);
        }
    }

    public TaskState state(String taskId) {
        AtomicReference<TaskState> state = states.get(taskId);
        if (state == null) {
            throw new NoSuchElementException(taskId);
        }
        return state.get();
    }

    public CompletableFuture<String> submit(Task task) {
        AtomicReference<TaskState> state = new AtomicReference<>(TaskState.PENDING);
        if (states.putIfAbsent(task.id(), state) != null) {
            throw new IllegalStateException("task already submitted: " + task.id());
        }
        CompletableFuture<String> result = CompletableFuture.supplyAsync(() -> execute(task), workers);
        result.whenComplete((value, error) -> state.set(error == null ? TaskState.COMPLETED : TaskState.FAILED));
        return result;
    }

    @Override
    public void close() {
        workers.shutdownNow();
    }

    private String execute(Task task) {
        String input = task.payload();
        for (String capability : task.route()) {
            input = call(capability, input);
        }
        return input;
    }

    private String call(String capability, String input) {
        List<Agent> peers = registry.getOrDefault(capability, List.of());
        if (peers.isEmpty()) {
            throw new IllegalStateException("no agent for " + capability);
        }
        RuntimeException last = new IllegalStateException("no peer");
        for (Agent peer : peers) {
            try {
                return peer.execute(input);
            } catch (RuntimeException error) {
                last = error;
            }
        }
        throw last;
    }
}
