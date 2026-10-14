package com.javagenai.kb.ingestion;

import com.javagenai.kb.chunking.SectionAwareChunker;
import com.javagenai.kb.domain.Chunk;
import com.javagenai.kb.domain.SourceDocument;
import com.javagenai.kb.embedding.NomicEmbeddingModel;
import com.javagenai.kb.retrieval.ChunkStore;
import dev.langchain4j.data.embedding.Embedding;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The ingest path end to end: parse, chunk, embed, persist. Every stage is a
 * separate bean so the eval harness can ablate them independently, and this
 * class is the only place that knows the order.
 */
@ApplicationScoped
public class IngestionPipeline {

    private final DocumentIngestor ingestor;
    private final SectionAwareChunker chunker;
    private final NomicEmbeddingModel embedder;
    private final ChunkStore store;

    @Inject
    public IngestionPipeline(
            DocumentIngestor ingestor,
            SectionAwareChunker chunker,
            NomicEmbeddingModel embedder,
            ChunkStore store) {
        this.ingestor = ingestor;
        this.chunker = chunker;
        this.embedder = embedder;
        this.store = store;
    }

    public IngestionReport ingest(SourceDocument source) {
        IngestedDocument document = ingestor.ingest(source);
        List<Chunk> chunks = chunker.chunk(document.structured(), document.allowedRoles());
        List<float[]> vectors = embed(chunks);
        store.save(document, chunks, vectors);
        return new IngestionReport(
                document.documentId(),
                document.title(),
                document.parser(),
                chunks.size(),
                document.structured().stats());
    }

    private List<float[]> embed(List<Chunk> chunks) {
        if (chunks.isEmpty()) {
            return List.of();
        }
        return embedder.embedChunks(chunks).stream()
                .map(Embedding::vector)
                .collect(Collectors.toList());
    }
}