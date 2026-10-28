package com.javagenai.lab10;

import com.javagenai.lab10.agent.IngestionAgent;
import com.javagenai.lab10.agent.ReasoningAgent;
import com.javagenai.lab10.agent.ValidationAgent;
import com.javagenai.lab10.control.AgentControlPlane;
import com.javagenai.lab10.task.Task;

import java.util.concurrent.CancellationException;

public final class Demo {

    public static void main(String[] args) throws InterruptedException {
        try (AgentControlPlane plane = new AgentControlPlane()) {
            plane.onEvent(event -> System.out.println("event " + event.state() + " " + event.detail()));
            plane.register(new IngestionAgent());
            plane.register(new ReasoningAgent("reasoning-primary", true));
            plane.register(new ReasoningAgent("reasoning-backup", false));
            plane.register(new ValidationAgent());

            System.out.println("-- run to completion --");
            Task done = Task.of("demo-1", "refund policy", "text-parsing", "inference", "validation");
            System.out.println("control plane complete: " + plane.submit(done).join());

            System.out.println("-- cancel during ingestion --");
            Task cancelled = Task.of("demo-2", "refund policy", "text-parsing", "inference", "validation");
            var running = plane.submit(cancelled);
            Thread.sleep(200);
            plane.cancel(cancelled.id());
            try {
                running.join();
            } catch (CancellationException ignored) {
                System.out.println("control plane stopped: " + plane.state(cancelled.id()));
            }
        }
    }
}
