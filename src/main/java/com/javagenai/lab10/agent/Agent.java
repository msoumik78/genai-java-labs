package com.javagenai.lab10.agent;

public interface Agent {
    AgentCard card();

    /** The plane asks before execute. False means try the next peer. */
    boolean accept(String capability);

    /** Returns the text the next capability receives. */
    String execute(String input);
}
