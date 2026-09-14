package com.javagenai.lab2.kb;

import com.javagenai.lab2.config.LabProperties;
import org.springframework.ai.embedding.EmbeddingModel;

public class TransformerEmbedder implements TextEmbedder {

    private final EmbeddingModel model;
    private final LabProperties lab;

    public TransformerEmbedder(EmbeddingModel model, LabProperties lab) {
        this.model = model;
        this.lab = lab;
    }

    @Override
    public float[] embedDocument(String text) {
        return Vectors.toFloatArray(model.embed(text));
    }

    @Override
    public float[] embedQuery(String text) {
        String prefix = lab.embedQueryPrefix();
        String input = (prefix == null || prefix.isBlank()) ? text : prefix + text;
        return Vectors.toFloatArray(model.embed(input));
    }

    @Override
    public int dimensions() {
        return lab.embeddingDims();
    }

    @Override
    public String name() {
        return lab.embedder();
    }
}
