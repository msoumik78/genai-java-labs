package com.javagenai.kb.generation;

/** One completion, with the usage the dashboard needs for cost. */
public record ChatReply(String text, int promptTokens, int completionTokens) {

    public ChatReply {
        text = text == null ? "" : text;
    }
}
