package com.javagenai.kb.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChunkStoreTest {

    @Test
    void formatsVectorsAsPgvectorLiterals() {
        String literal = ChunkStore.toVectorLiteral(new float[] {0.5f, -1f, 3.25f});

        assertThat(literal).isEqualTo("[0.5,-1.0,3.25]");
    }

    @Test
    void producesBracketFormForAFiftyTwelveDimensionVector() {
        float[] vector = new float[512];

        String literal = ChunkStore.toVectorLiteral(vector);

        // "[0.0,0.0,...]" — 512 elements of "0.0" joined by 511 commas, plus brackets.
        assertThat(literal).isEqualTo("[" + "0.0,".repeat(511) + "0.0]");
    }
}