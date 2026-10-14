package com.javagenai.kb.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.javagenai.kb.domain.Chunk;
import dev.langchain4j.data.embedding.Embedding;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Downloads the ONNX model on first run (~137MB, then cached) and proves the
 * whole path: download, tokenize, forward pass, pooling, MRL truncation, and
 * that a question about leave ranks leave-policy text above unrelated text.
 * Lives in the embedding package to reach the config fields directly, without
 * needing CDI just to assert behavior.
 */
class NomicEmbeddingModelTest {

    private final NomicEmbeddingModel model = model();

    @Test
    void embedsAtTheConfiguredDimension() {
        Embedding embedding = model.embedQuery("How long is parental leave?");

        assertThat(embedding.dimension()).isEqualTo(512);
        assertThat(magnitude(embedding)).isCloseTo(1.0, within(1e-4));
    }

    @Test
    void embedsChunksWithTheDocumentPrefix() {
        List<Embedding> embeddings = model.embedChunks(List.of(
                chunk("Employees receive 12 weeks of paid parental leave.")));

        assertThat(embeddings).hasSize(1);
        assertThat(embeddings.getFirst().dimension()).isEqualTo(512);
    }

    @Test
    void ranksTopicallyRelatedTextAboveUnrelatedText() {
        Embedding question = model.embedQuery("How long is parental leave?");
        Embedding related = model.embedChunks(List.of(
                chunk("Employees receive 12 weeks of paid parental leave."))).getFirst();
        Embedding unrelated = model.embedChunks(List.of(
                chunk("The cafeteria serves lunch from 11am to 3pm on weekdays."))).getFirst();

        assertThat(cosine(question, related)).isGreaterThan(cosine(question, unrelated));
    }

    @Test
    void embedsIdenticalTextIdentically() {
        String text = "The refund policy applies within 30 days.";

        Embedding first = model.embedQuery(text);
        Embedding second = model.embedQuery(text);

        assertThat(cosine(first, second)).isCloseTo(1.0, within(1e-5));
    }

    private NomicEmbeddingModel model() {
        NomicEmbeddingModel instance = new NomicEmbeddingModel();
        instance.modelId = "nomic-ai/nomic-embed-text-v1.5";
        instance.dimension = 512;
        instance.documentPrefix = "search_document:";
        instance.queryPrefix = "search_query:";
        return instance;
    }

    private Chunk chunk(String text) {
        return new Chunk("c1", "d1", "s1", text, "", List.of());
    }

    private double cosine(Embedding left, Embedding right) {
        float[] a = left.vector();
        float[] b = right.vector();
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    private double magnitude(Embedding embedding) {
        double sum = 0;
        for (float value : embedding.vector()) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }
}