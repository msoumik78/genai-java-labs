package com.javagenai.rag.corpus;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class TextWindowChunkerTest {

    private final TextWindowChunker chunker = new TextWindowChunker();

    @Test
    void shortDocumentStaysOnePassage() {
        List<Passage> passages = chunker.windows(
                List.of(new CorpusDocument("Warranty", "The gasket is ZX-4419.")),
                700,
                120);

        assertThat(passages).hasSize(1);
        assertThat(passages.getFirst().passageId()).isEqualTo("Warranty#1");
        assertThat(passages.getFirst().text()).contains("ZX-4419");
    }

    @Test
    void longerDocumentOverlapsAndDoesNotCutTheLastWord() {
        String body = "alpha ".repeat(80) + "omega-marker " + "beta ".repeat(80);
        List<Passage> passages = chunker.windows(
                List.of(new CorpusDocument("Dispatch", body)),
                200,
                40);

        assertThat(passages.size()).isGreaterThan(1);
        assertThat(passages.getFirst().text()).doesNotEndWith("alph");
        String joined = String.join(" ", passages.stream().map(Passage::text).toList());
        assertThat(joined).contains("omega-marker");
    }
}
