package com.javagenai.lab7.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class VectorsTest {

    @Test
    void truncatedVectorIsUnitLength() {
        float[] vector = {3, 4, 0};
        float[] truncated = Vectors.truncateAndNormalize(vector, 2);

        assertThat(truncated).hasSize(2);
        assertThat(Vectors.dot(truncated, truncated)).isCloseTo(1.0, within(1e-5));
    }
}
