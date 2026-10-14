package com.javagenai.kb.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MrlTest {

    @Test
    void truncatesToTheRequestedDimension() {
        float[] vector = unitLength(768);

        float[] truncated = Mrl.truncate(vector, 512);

        assertThat(truncated).hasSize(512);
    }

    @Test
    void renormalizesAfterTruncation() {
        float[] vector = unitLength(768);

        float[] truncated = Mrl.truncate(vector, 128);

        assertThat(magnitude(truncated)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
    }

    @Test
    void keepsFullVectorWhenTargetExceedsModel() {
        float[] vector = unitLength(768);

        assertThat(Mrl.truncate(vector, 1024)).isSameAs(vector);
    }

    @Test
    void rejectsDimensionsNomicWasNotTrainedFor() {
        float[] vector = unitLength(768);

        assertThatThrownBy(() -> Mrl.truncate(vector, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("multiple of 64");
    }

    @Test
    void supportsEveryDocumentedNomicSize() {
        float[] vector = unitLength(768);

        for (int size : new int[] {512, 256, 128, 64}) {
            assertThat(Mrl.truncate(vector, size)).hasSize(size);
        }
    }

    private float[] unitLength(int size) {
        float[] vector = new float[size];
        vector[0] = 1f;
        return vector;
    }

    private double magnitude(float[] vector) {
        double sum = 0;
        for (float value : vector) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }
}