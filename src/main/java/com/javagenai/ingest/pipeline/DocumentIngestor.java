package com.javagenai.ingest.pipeline;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.nio.file.Path;
import java.util.List;

public final class DocumentIngestor {

    private final EmbeddingModel model;
    private final DocumentSplitter splitter;

    public DocumentIngestor(EmbeddingModel model, DocumentSplitter splitter) {
        this.model = model;
        this.splitter = splitter;
    }

    public List<String> ingest(Path directory, EmbeddingStore<TextSegment> store) {
        store.removeAll();
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(directory);
        List<TextSegment> segments = splitter.splitAll(documents);
        if (segments.isEmpty()) {
            return List.of();
        }
        List<Embedding> embeddings = model.embedAll(segments).content();
        store.addAll(embeddings, segments);
        return segments.stream().map(TextSegment::text).toList();
    }

    public List<String> search(EmbeddingStore<TextSegment> store, String query, int maxResults) {
        Embedding queryEmbedding = model.embed(query).content();
        EmbeddingSearchResult<TextSegment> result = store.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(maxResults)
                        .build());
        return result.matches().stream().map(match -> match.embedded().text()).toList();
    }
}
