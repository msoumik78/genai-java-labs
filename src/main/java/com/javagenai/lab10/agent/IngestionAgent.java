package com.javagenai.lab10.agent;

import java.util.Set;

public final class IngestionAgent implements Agent {

    private final AgentCard card = new AgentCard("ingestion", Set.of("text-parsing"));

    @Override
    public AgentCard card() {
        return card;
    }

    @Override
    public String execute(String input) {
        System.out.println("IngestionAgent complete");
        return input;
    }
}
