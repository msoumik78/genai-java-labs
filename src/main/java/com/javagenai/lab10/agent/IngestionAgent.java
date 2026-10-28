package com.javagenai.lab10.agent;

import java.util.Set;

public final class IngestionAgent implements Agent {

    private final AgentCard card = new AgentCard("ingestion", Set.of("text-parsing"));

    @Override
    public AgentCard card() {
        return card;
    }

    @Override
    public boolean accept(String capability) {
        return card.capabilities().contains(capability);
    }

    @Override
    public String execute(String input) {
        pause();
        System.out.println("IngestionAgent complete");
        return input;
    }

    private static void pause() {
        try {
            Thread.sleep(800);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
