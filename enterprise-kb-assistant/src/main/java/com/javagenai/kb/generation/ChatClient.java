package com.javagenai.kb.generation;

/**
 * One chat completion. The generator and the judge share this so tests can
 * stub the model, and so the two calls can target different model ids.
 */
public interface ChatClient {

    ChatReply complete(String model, double temperature, String system, String user);
}
