package com.javagenai.lab10.agent;

import java.util.Set;

public final class ReasoningAgent implements Agent {

    private final AgentCard card;
    private final boolean fail;

    public ReasoningAgent(String id, boolean fail) {
        this.card = new AgentCard(id, Set.of("inference"));
        this.fail = fail;
    }

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
        if (fail) {
            System.out.println(card.id() + " failed");
            throw new IllegalStateException(card.id() + " failed");
        }
        System.out.println(card.id() + " complete");
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
