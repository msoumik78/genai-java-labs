package com.javagenai.lab10.agent;

public interface Agent {
    AgentCard card();

    /** Returns the text the next capability receives. */
    String execute(String input);
}
