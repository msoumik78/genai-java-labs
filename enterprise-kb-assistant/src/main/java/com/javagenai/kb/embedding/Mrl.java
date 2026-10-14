package com.javagenai.kb.embedding;

/**
 * Matryoshka Representation Learning truncation. Nomic models are trained so the
 * first N dimensions form a valid standalone vector, letting one embedding pass
 * serve several storage sizes. Truncation must be followed by renormalization,
 * otherwise cosine similarity compares vectors of different lengths.
 */
public final class Mrl {

    private Mrl() {
    }

    /**
     * Keeps the first {@code targetDimensions} values and renormalizes.
     * Nomic supports 768, 512, 256, 128 and 64, all multiples of 64.
     */
    public static float[] truncate(float[] vector, int targetDimensions) {
        if (targetDimensions <= 0 || targetDimensions >= vector.length) {
            return vector;
        }
        if (targetDimensions % 64 != 0) {
            throw new IllegalArgumentException(
                    "Nomic MRL dimensions must be a multiple of 64, got " + targetDimensions);
        }
        float[] truncated = new float[targetDimensions];
        System.arraycopy(vector, 0, truncated, 0, targetDimensions);
        l2Normalize(truncated);
        return truncated;
    }

    private static void l2Normalize(float[] vector) {
        double magnitude = 0;
        for (float value : vector) {
            magnitude += value * value;
        }
        magnitude = Math.sqrt(magnitude);
        if (magnitude == 0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / magnitude);
        }
    }
}