package com.javagenai.lab10.control;

import com.javagenai.lab10.agent.Agent;
import com.javagenai.lab10.agent.AgentCard;
import com.javagenai.lab10.task.Task;
import com.javagenai.lab10.task.TaskState;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentControlPlaneTest {

    @Test
    void failedPeerFallsThroughToTheNext() {
        try (AgentControlPlane plane = new AgentControlPlane()) {
            plane.register(agent("primary", "inference", true, input -> {
                throw new IllegalStateException("primary down");
            }));
            plane.register(agent("secondary", "inference", true, input -> "ok:" + input));

            String output = plane.submit(Task.of("t-1", "policy", "inference")).join();

            assertEquals("ok:policy", output);
            assertEquals(TaskState.COMPLETED, plane.state("t-1"));
            assertEquals(TaskState.PENDING, plane.events().getFirst().state());
            assertEquals(TaskState.COMPLETED, plane.events().getLast().state());
        }
    }

    @Test
    void refusedPeerIsSkipped() {
        try (AgentControlPlane plane = new AgentControlPlane()) {
            plane.register(agent("busy", "inference", false, input -> {
                throw new IllegalStateException("should not run");
            }));
            plane.register(agent("ready", "inference", true, input -> "ok:" + input));

            assertEquals("ok:policy", plane.submit(Task.of("t-2", "policy", "inference")).join());
            assertEquals(TaskState.COMPLETED, plane.state("t-2"));
        }
    }

    @Test
    void cancelStopsTheNextCapability() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger validated = new AtomicInteger();

        try (AgentControlPlane plane = new AgentControlPlane()) {
            plane.register(agent("slow", "text-parsing", true, input -> {
                started.countDown();
                release.await(2, TimeUnit.SECONDS);
                return input;
            }));
            plane.register(agent("checker", "validation", true, input -> {
                validated.incrementAndGet();
                return input;
            }));

            CompletableFuture<String> future = plane.submit(
                    Task.of("t-3", "policy", "text-parsing", "validation"));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertTrue(plane.cancel("t-3"));
            release.countDown();

            assertThrows(CancellationException.class, future::join);
            assertEquals(TaskState.CANCELLED, plane.state("t-3"));
            assertEquals(0, validated.get());
        }
    }

    @Test
    void completedTaskStaysCompleted() {
        try (AgentControlPlane plane = new AgentControlPlane()) {
            plane.register(agent("parser", "text-parsing", true, input -> input));
            plane.submit(Task.of("t-4", "policy", "text-parsing")).join();

            assertFalse(plane.cancel("t-4"));
            assertEquals(TaskState.COMPLETED, plane.state("t-4"));
        }
    }

    @Test
    void failedCapabilityFailsTheTask() {
        try (AgentControlPlane plane = new AgentControlPlane()) {
            plane.register(agent("primary", "inference", true, input -> {
                throw new IllegalStateException("primary down");
            }));

            CompletionException error = assertThrows(CompletionException.class,
                    () -> plane.submit(Task.of("t-5", "policy", "inference")).join());

            assertTrue(error.getCause() instanceof IllegalStateException);
            assertEquals(TaskState.FAILED, plane.state("t-5"));
        }
    }

    private static Agent agent(String id, String capability, boolean accept, Step step) {
        return new Agent() {
            private final AgentCard card = new AgentCard(id, Set.of(capability));

            @Override
            public AgentCard card() {
                return card;
            }

            @Override
            public boolean accept(String offered) {
                return accept && card.capabilities().contains(offered);
            }

            @Override
            public String execute(String input) {
                try {
                    return step.apply(input);
                } catch (RuntimeException error) {
                    throw error;
                } catch (Exception error) {
                    throw new IllegalStateException(error);
                }
            }
        };
    }

    @FunctionalInterface
    private interface Step {
        String apply(String input) throws Exception;
    }
}
