package com.javagenai.kb.retrieval;

import com.javagenai.kb.domain.Chunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal Rank Fusion: score = sum over rankings of 1 / (k + rank).
 *
 * <p>Used instead of averaging similarity scores because cosine and BM25 are
 * on incomparable scales; ranks are the only signal both sides share. k=60 is
 * the standard constant, favoring items that rank well in both lists.
 */
public final class ReciprocalRankFusion {

    private final double k;

    public ReciprocalRankFusion(double k) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be positive, got " + k);
        }
        this.k = k;
    }

    /** Fuses two ranked lists; ties break by chunk id so runs are reproducible. */
    public List<RetrievalResult> fuse(List<Chunk> vectorRanked, List<Chunk> lexicalRanked) {
        Map<String, Double> scores = new HashMap<>();
        Map<String, Integer> vectorRanks = rankByPosition(vectorRanked);
        Map<String, Integer> lexicalRanks = rankByPosition(lexicalRanked);
        Map<String, Chunk> chunks = index(vectorRanked, lexicalRanked);

        addScores(scores, vectorRanks);
        addScores(scores, lexicalRanks);

        List<RetrievalResult> fused = new ArrayList<>();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            String chunkId = entry.getKey();
            fused.add(new RetrievalResult(
                    chunks.get(chunkId),
                    entry.getValue(),
                    vectorRanks.getOrDefault(chunkId, 0),
                    lexicalRanks.getOrDefault(chunkId, 0)));
        }
        fused.sort(Comparator.comparingDouble(RetrievalResult::fusionScore).reversed()
                .thenComparing(result -> result.chunk().id()));
        return fused;
    }

    private Map<String, Integer> rankByPosition(List<Chunk> ranked) {
        Map<String, Integer> ranks = new LinkedHashMap<>();
        for (int i = 0; i < ranked.size(); i++) {
            ranks.putIfAbsent(ranked.get(i).id(), i + 1);
        }
        return ranks;
    }

    private Map<String, Chunk> index(List<Chunk>... lists) {
        Map<String, Chunk> chunks = new LinkedHashMap<>();
        for (List<Chunk> list : lists) {
            for (Chunk chunk : list) {
                chunks.putIfAbsent(chunk.id(), chunk);
            }
        }
        return chunks;
    }

    private void addScores(Map<String, Double> scores, Map<String, Integer> ranks) {
        for (Map.Entry<String, Integer> entry : ranks.entrySet()) {
            scores.merge(entry.getKey(), 1.0 / (k + entry.getValue()), Double::sum);
        }
    }
}