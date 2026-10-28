package com.javagenai.lab10.agent;

import java.util.Set;

public final class ValidationAgent implements Agent {

    private final AgentCard card = new AgentCard("validation", Set.of("validation"));

    @Override
    public AgentCard card() {
        return card;
    }

    @Override
    public String execute(String input) {
        System.out.println("ValidationAgent complete");
        return input;
    }
}
