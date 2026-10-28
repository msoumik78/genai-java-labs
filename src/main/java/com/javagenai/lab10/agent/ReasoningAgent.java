package com.javagenai.lab10.agent;

import java.util.Set;

public final class ReasoningAgent implements Agent {

    private final AgentCard card = new AgentCard("reasoning", Set.of("inference"));

    @Override
    public AgentCard card() {
        return card;
    }

    @Override
    public String execute(String input) {
        System.out.println("ReasoningAgent complete");
        return input;
    }
}
