package com.javagenai.rag.answer;

import java.util.List;

public record RagAnswer(
        String question,
        String answer,
        List<RetrievedPassage> vectorHits,
        List<RetrievedPassage> lexicalHits,
        List<RetrievedPassage> fusedHits,
        List<RetrievedPassage> rerankedHits,
        List<RetrievedPassage> passagesGivenToModel) {
}
