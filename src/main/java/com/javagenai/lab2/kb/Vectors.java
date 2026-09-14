package com.javagenai.lab2.kb;

public final class Vectors {

    private Vectors() {
    }

    public static String toLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 8);
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }

    public static float[] toFloatArray(float[] v) {
        return v;
    }

    public static float[] toFloatArray(double[] v) {
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            out[i] = (float) v[i];
        }
        return out;
    }
}
