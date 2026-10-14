package com.javagenai.lab7.embedding;

import com.javagenai.lab7.config.LabProperties;
import org.junit.jupiter.api.Test;

class OnnxNomicEmbedderTest {

    @Test
    void paraphrasesAreCloserThanAnUnrelatedQuestion() {
        LabProperties lab = new LabProperties();
        OnnxNomicEmbedder embedder = new OnnxNomicEmbedder(lab);
        float[] a = embedder.embed("What is a semantic cache?");
        float[] b = embedder.embed("Explain semantic caching");
        float[] c = embedder.embed("How do I boil pasta?");
        double paraphrase = Vectors.dot(a, b);
        double unrelated = Vectors.dot(a, c);
        org.assertj.core.api.Assertions.assertThat(paraphrase).isGreaterThan(0.80);
        org.assertj.core.api.Assertions.assertThat(unrelated).isLessThan(0.60);
    }
}
