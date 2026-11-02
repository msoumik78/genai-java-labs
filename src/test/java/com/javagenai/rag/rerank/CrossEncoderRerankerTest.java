package com.javagenai.rag.rerank;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.javagenai.rag.config.RagSettings;
import com.javagenai.rag.corpus.Passage;
import com.javagenai.rag.retrieval.RankedPassage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class CrossEncoderRerankerTest {

    @Test
    @EnabledIf("modelPresent")
    void relevantPassageScoresAboveAnUnrelatedPassage() {
        CrossEncoderReranker reranker = new CrossEncoderReranker(settings());
        List<RankedPassage> reranked = reranker.rerank(
                "Can a sealed solvent kit be returned?",
                List.of(
                        new RankedPassage(new Passage("other", "Dispatch",
                                "The night visit fee is ninety-five dollars after 18:00."), 0),
                        new RankedPassage(new Passage("returns", "Parts returns",
                                "Sealed solvent kits cannot be sent back. An unopened kit is still final sale."), 0)));

        assertThat(reranked.getFirst().passage().passageId()).isEqualTo("returns");
        assertThat(reranked.getFirst().score()).isGreaterThan(reranked.get(1).score());
        reranker.close();
    }

    static boolean modelPresent() {
        Path directory = Path.of("models/ms-marco-MiniLM-L6");
        return Files.isRegularFile(directory.resolve("model.onnx"))
                && Files.isRegularFile(directory.resolve("tokenizer.json"));
    }

    private static RagSettings settings() {
        return new RagSettings(700, 120, 8, 8, 60, 8, 3, "models/ms-marco-MiniLM-L6");
    }
}
