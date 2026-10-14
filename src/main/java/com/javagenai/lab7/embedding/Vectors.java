package com.javagenai.lab7.embedding;

public final class Vectors {

    private Vectors() {
    }

    public static float[] meanPool(float[] tokenVectors, long[] attentionMask, int hiddenSize) {
        float[] sum = new float[hiddenSize];
        int counted = 0;
        int tokens = attentionMask.length;
        for (int token = 0; token < tokens; token++) {
            if (attentionMask[token] == 0) {
                continue;
            }
            int offset = token * hiddenSize;
            for (int dim = 0; dim < hiddenSize; dim++) {
                sum[dim] += tokenVectors[offset + dim];
            }
            counted++;
        }
        if (counted == 0) {
            throw new IllegalArgumentException("Embedding attention mask is empty");
        }
        for (int dim = 0; dim < hiddenSize; dim++) {
            sum[dim] /= counted;
        }
        return sum;
    }

    /** Keep the leading Matryoshka dimensions and L2-normalize again. */
    public static float[] truncateAndNormalize(float[] vector, int dimensions) {
        if (dimensions > vector.length) {
            throw new IllegalArgumentException("Requested " + dimensions + " dims from a " + vector.length + " vector");
        }
        float[] sliced = new float[dimensions];
        System.arraycopy(vector, 0, sliced, 0, dimensions);
        normalize(sliced);
        return sliced;
    }

    public static void normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) {
            sum += value * value;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= (float) norm;
        }
    }

    public static double dot(float[] left, float[] right) {
        double sum = 0;
        for (int i = 0; i < left.length; i++) {
            sum += left[i] * (double) right[i];
        }
        return sum;
    }
}
