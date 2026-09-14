package com.javagenai.lab2.kb;

public interface TextEmbedder {

    float[] embedDocument(String text);

    float[] embedQuery(String text);

    int dimensions();

    String name();
}
