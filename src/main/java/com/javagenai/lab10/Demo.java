package com.javagenai.lab10;

import com.javagenai.lab10.agent.IngestionAgent;
import com.javagenai.lab10.agent.ReasoningAgent;
import com.javagenai.lab10.agent.ValidationAgent;
import com.javagenai.lab10.control.AgentControlPlane;
import com.javagenai.lab10.task.Task;

public final class Demo {

    public static void main(String[] args) {
        try (AgentControlPlane plane = new AgentControlPlane()) {
            plane.register(new IngestionAgent());
            plane.register(new ReasoningAgent());
            plane.register(new ValidationAgent());
            Task task = Task.of("demo-1", "refund policy", "text-parsing", "inference", "validation");
            String output = plane.submit(task).join();
            System.out.println("control plane complete: " + output);
        }
    }
}
