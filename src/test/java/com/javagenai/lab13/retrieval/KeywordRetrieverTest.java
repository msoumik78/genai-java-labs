package com.javagenai.lab13.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KeywordRetrieverTest {

    private final KeywordRetriever retriever = new KeywordRetriever();

    @Test
    void ranksTheRefundPassageFirst() {
        var contexts = retriever.retrieve("How many business days until a refund arrives?", 2);

        assertThat(contexts).hasSize(2);
        assertThat(contexts.getFirst()).contains("5 business days");
    }

    @Test
    void stillReturnsPassagesWhenNothingMatches() {
        var contexts = retriever.retrieve("guest wifi password", 2);

        assertThat(contexts).hasSize(2);
        assertThat(String.join(" ", contexts)).doesNotContain("wifi");
    }
}
