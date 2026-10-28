package com.javagenai.lab10.control;

import com.javagenai.lab10.agent.Agent;
import com.javagenai.lab10.agent.AgentCard;
import com.javagenai.lab10.task.Task;
import com.javagenai.lab10.task.TaskState;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentControlPlaneTest {

    @Test
    void fallbackPeerCompletesTheTask() {
        try (AgentControlPlane plane = new AgentControlPlane()) {
            plane.register(agent("primary", "inference", input -> {
                throw new IllegalStateException("primary down");
            }));
            plane.register(agent("secondary", "inference", input -> "ok:" + input));

            String output = plane.submit(Task.of("t-1", "policy", "inference")).join();

            assertEquals("ok:policy", output);
            assertEquals(TaskState.COMPLETED, plane.state("t-1"));
        }
    }

    @Test
    void failedCapabilityFailsTheTask() {
        try (AgentControlPlane plane = new AgentControlPlane()) {
            plane.register(agent("primary", "inference", input -> {
                throw new IllegalStateException("primary down");
            }));

            CompletionException error = assertThrows(CompletionException.class,
                    () -> plane.submit(Task.of("t-2", "policy", "inference")).join());

            assertTrue(error.getCause() instanceof IllegalStateException);
            assertEquals(TaskState.FAILED, plane.state("t-2"));
        }
    }

    private static Agent agent(String id, String capability, Step step) {
        return new Agent() {
            private final AgentCard card = new AgentCard(id, Set.of(capability));

            @Override
            public AgentCard card() {
                return card;
            }

            @Override
            public String execute(String input) {
                return step.apply(input);
            }
        };
    }

    @FunctionalInterface
    private interface Step {
        String apply(String input);
    }
}
