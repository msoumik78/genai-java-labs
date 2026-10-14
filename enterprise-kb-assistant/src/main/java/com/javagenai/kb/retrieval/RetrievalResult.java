package com.javagenai.kb.retrieval;

import com.javagenai.kb.domain.Chunk;

/**
 * A chunk that survived retrieval, carrying both fusion inputs so the
 * dashboard can show *why* something ranked where it did.
 */
public record RetrievalResult(
        Chunk chunk,
        double fusionScore,
        int vectorRank,
        int lexicalRank) {

    public boolean foundByVector() {
        return vectorRank > 0;
    }

    public boolean foundLexically() {
        return lexicalRank > 0;
    }
}