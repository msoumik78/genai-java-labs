package com.javagenai.kb.generation;

import java.util.List;

/**
 * What the generator produced for one question. Citations are the chunk ids
 * behind the [n] markers, in the order the markers appear.
 */
public record GeneratedAnswer(
        String text,
        boolean abstained,
        List<String> citedChunkIds,
        boolean citationsValid,
        int promptTokens,
        int completionTokens,
        long latencyMs) {

    public GeneratedAnswer {
        text = text == null ? "" : text;
        citedChunkIds = List.copyOf(citedChunkIds == null ? List.of() : citedChunkIds);
    }
}
