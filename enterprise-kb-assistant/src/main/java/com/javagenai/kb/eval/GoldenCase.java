package com.javagenai.kb.eval;

import java.util.List;

/**
 * One line of {@code golden.jsonl}. Recall is graded against document ids
 * rather than chunk ids because chunk ids depend on parse internals and would
 * make the dataset brittle across Docling versions.
 */
public record GoldenCase(
        String id,
        String question,
        String role,
        String category,
        List<String> supportingDocs,
        List<String> mustContain,
        Boolean expectAbstain) {

    public GoldenCase {
        supportingDocs = supportingDocs == null ? List.of() : List.copyOf(supportingDocs);
        mustContain = mustContain == null ? List.of() : List.copyOf(mustContain);
    }

    public boolean expectsAbstain() {
        return Boolean.TRUE.equals(expectAbstain);
    }
}