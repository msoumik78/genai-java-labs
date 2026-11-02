package com.javagenai.rag.retrieval;

import com.javagenai.rag.corpus.Passage;

public record RankedPassage(Passage passage, double score) {
}
