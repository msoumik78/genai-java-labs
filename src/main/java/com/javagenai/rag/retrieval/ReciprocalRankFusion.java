package com.javagenai.rag.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReciprocalRankFusion {

    private ReciprocalRankFusion() {
    }

    public static List<RankedPassage> fuse(
            List<RankedPassage> vectorHits,
            List<RankedPassage> lexicalHits,
            int rankConstant,
            int limit) {
        Map<String, PassageScore> fused = new LinkedHashMap<>();
        accumulate(fused, vectorHits, rankConstant);
        accumulate(fused, lexicalHits, rankConstant);
        return fused.values().stream()
                .sorted(Comparator.comparingDouble(PassageScore::score).reversed())
                .limit(limit)
                .map(entry -> new RankedPassage(entry.passage(), entry.score()))
                .toList();
    }

    private static void accumulate(Map<String, PassageScore> fused, List<RankedPassage> hits, int rankConstant) {
        for (int index = 0; index < hits.size(); index++) {
            RankedPassage hit = hits.get(index);
            int oneBasedRank = index + 1;
            double contribution = 1.0 / (rankConstant + oneBasedRank);
            fused.merge(
                    hit.passage().passageId(),
                    new PassageScore(hit.passage(), contribution),
                    (existing, incoming) -> new PassageScore(existing.passage(), existing.score() + incoming.score()));
        }
    }

    private record PassageScore(com.javagenai.rag.corpus.Passage passage, double score) {
    }
}
