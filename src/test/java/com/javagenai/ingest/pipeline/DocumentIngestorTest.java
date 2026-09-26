package com.javagenai.ingest.pipeline;

import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentIngestorTest {

    @Test
    void ingestAndSearchWithInMemoryStore(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("policy.txt"),
                "Refund policy: tickets can be refunded within 14 days of purchase.");
        EmbeddingStore<TextSegment> memory = new InMemoryEmbeddingStore<>();
        DocumentIngestor ingestor = new DocumentIngestor(
                new AllMiniLmL6V2QuantizedEmbeddingModel(),
                DocumentSplitters.recursive(300, 30));

        List<String> inserted = ingestor.ingest(dir, memory);
        String hit = ingestor.search(memory, "refund window", 1).getFirst();

        assertFalse(inserted.isEmpty());
        assertTrue(hit.contains("14 days"));

        Files.writeString(dir.resolve("policy.txt"), "Baggage policy: one cabin bag is included.");
        List<String> replaced = ingestor.ingest(dir, memory);
        String after = ingestor.search(memory, "refund window", 1).getFirst();

        assertTrue(replaced.getFirst().contains("cabin bag"));
        assertFalse(after.contains("14 days"));
    }
}
