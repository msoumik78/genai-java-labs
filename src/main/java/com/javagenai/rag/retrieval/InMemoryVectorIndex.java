package com.javagenai.rag.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.javagenai.rag.corpus.Passage;
import org.springframework.ai.embedding.EmbeddingModel;

public final class InMemoryVectorIndex {

    private final EmbeddingModel embeddingModel;
    private final List<Passage> passages;
    private final List<float[]> normalizedVectors;

    private InMemoryVectorIndex(EmbeddingModel embeddingModel, List<Passage> passages, List<float[]> normalizedVectors) {
        this.embeddingModel = embeddingModel;
        this.passages = passages;
        this.normalizedVectors = normalizedVectors;
    }

    public static InMemoryVectorIndex build(EmbeddingModel embeddingModel, List<Passage> passages) {
        List<String> textsToEmbed = passages.stream()
                .map(passage -> NomicEmbeddingPrefixes.DOCUMENT + passage.text())
                .toList();
        List<float[]> vectors = embeddingModel.embed(textsToEmbed);
        if (vectors.size() != passages.size()) {
            throw new IllegalStateException("Embedding model returned " + vectors.size()
                    + " vectors for " + passages.size() + " passages");
        }
        List<float[]> normalized = vectors.stream().map(InMemoryVectorIndex::normalize).toList();
        return new InMemoryVectorIndex(embeddingModel, List.copyOf(passages), normalized);
    }

    public List<RankedPassage> search(String question, int limit) {
        float[] query = normalize(embeddingModel.embed(NomicEmbeddingPrefixes.QUERY + question));
        List<RankedPassage> ranked = new ArrayList<>();
        for (int index = 0; index < passages.size(); index++) {
            ranked.add(new RankedPassage(passages.get(index), dotProduct(query, normalizedVectors.get(index))));
        }
        ranked.sort(Comparator.comparingDouble(RankedPassage::score).reversed());
        return ranked.stream().limit(limit).toList();
    }

    private static float[] normalize(float[] vector) {
        double sumOfSquares = 0;
        for (float component : vector) {
            sumOfSquares += component * component;
        }
        double magnitude = Math.sqrt(sumOfSquares);
        if (magnitude == 0) {
            return vector.clone();
        }
        float[] normalized = new float[vector.length];
        for (int index = 0; index < vector.length; index++) {
            normalized[index] = (float) (vector[index] / magnitude);
        }
        return normalized;
    }

    private static double dotProduct(float[] left, float[] right) {
        int length = Math.min(left.length, right.length);
        double sum = 0;
        for (int index = 0; index < length; index++) {
            sum += left[index] * right[index];
        }
        return sum;
    }
}
