package com.javagenai.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.javagenai.rag.corpus.Passage;
import org.junit.jupiter.api.Test;

class ReciprocalRankFusionTest {

    @Test
    void passageFoundByBothListsOutranksASingleListHit() {
        Passage shared = passage("shared");
        Passage vectorOnly = passage("vector-only");
        Passage lexicalOnly = passage("lexical-only");

        List<RankedPassage> fused = ReciprocalRankFusion.fuse(
                List.of(new RankedPassage(shared, 0.9), new RankedPassage(vectorOnly, 0.2)),
                List.of(new RankedPassage(lexicalOnly, 12.0), new RankedPassage(shared, 3.0)),
                60,
                3);

        assertThat(fused).extracting(hit -> hit.passage().passageId())
                .containsExactly("shared", "lexical-only", "vector-only");
        assertThat(fused.getFirst().score()).isGreaterThan(fused.get(1).score());
    }

    private static Passage passage(String id) {
        return new Passage(id, "Source", "text " + id);
    }
}
