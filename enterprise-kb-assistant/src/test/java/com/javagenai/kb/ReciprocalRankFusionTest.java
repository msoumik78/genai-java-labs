package com.javagenai.kb.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.javagenai.kb.domain.Chunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReciprocalRankFusionTest {

    private final ReciprocalRankFusion fusion = new ReciprocalRankFusion(60);

    @Test
    void favorsChunksThatRankWellInBothLists() {
        Chunk agreedOn = chunk("a");
        Chunk vectorOnly = chunk("b");
        Chunk lexicalOnly = chunk("c");

        List<RetrievalResult> fused = fusion.fuse(
                List.of(vectorOnly, agreedOn),
                List.of(lexicalOnly, agreedOn));

        assertThat(fused.getFirst().chunk()).isSameAs(agreedOn);
    }

    @Test
    void aChunkFoundByOnlyOneArmStillAppears() {
        List<RetrievalResult> fused = fusion.fuse(
                List.of(chunk("only-vector")),
                List.of(chunk("only-lexical")));

        assertThat(fused).hasSize(2);
        assertThat(fused.stream().map(r -> r.chunk().id()))
                .containsExactlyInAnyOrder("only-vector", "only-lexical");
    }

    @Test
    void recordsWhichArmsFoundEachChunk() {
        List<RetrievalResult> fused = fusion.fuse(
                List.of(chunk("both"), chunk("vec")),
                List.of(chunk("both"), chunk("lex")));

        RetrievalResult both = find(fused, "both");
        RetrievalResult vectorOnly = find(fused, "vec");
        RetrievalResult lexicalOnly = find(fused, "lex");

        assertThat(both.foundByVector()).isTrue();
        assertThat(both.foundLexically()).isTrue();
        assertThat(vectorOnly.foundLexically()).isFalse();
        assertThat(lexicalOnly.foundByVector()).isFalse();
    }

    @Test
    void tieBreaksDeterministicallySoRunsReproduce() {
        List<RetrievalResult> first = fusion.fuse(List.of(chunk("b"), chunk("a")), List.of());
        List<RetrievalResult> second = fusion.fuse(List.of(chunk("b"), chunk("a")), List.of());

        // Streams must be collected before comparing: a Stream has no content equality.
        List<String> firstOrder = ids(first);
        List<String> secondOrder = ids(second);

        assertThat(firstOrder).isEqualTo(secondOrder).isEqualTo(List.of("b", "a"));
    }

    private List<String> ids(List<RetrievalResult> results) {
        return results.stream().map(result -> result.chunk().id()).toList();
    }

    @Test
    void rejectsNonPositiveK() {
        assertThatThrownBy(() -> new ReciprocalRankFusion(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RetrievalResult find(List<RetrievalResult> results, String id) {
        return results.stream().filter(r -> r.chunk().id().equals(id)).findFirst().orElseThrow();
    }

    private Chunk chunk(String id) {
        return new Chunk(id, "doc", "sec", "text", "", List.of());
    }
}