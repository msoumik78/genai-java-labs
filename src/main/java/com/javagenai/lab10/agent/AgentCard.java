package com.javagenai.lab10.agent;

import java.util.Set;

/** What an agent publishes so peers can be discovered by capability. */
public record AgentCard(String id, Set<String> capabilities) {
    public AgentCard {
        capabilities = Set.copyOf(capabilities);
    }
}
