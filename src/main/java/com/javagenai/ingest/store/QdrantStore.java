package com.javagenai.ingest.store;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;

public final class QdrantStore implements EmbeddingStoreStrategy {

    private final String host;
    private final int port;
    private final String collectionName;

    public QdrantStore(String host, int port, String collectionName) {
        this.host = host;
        this.port = port;
        this.collectionName = collectionName;
    }

    @Override
    public String name() {
        return "qdrant";
    }

    @Override
    public EmbeddingStore<TextSegment> open() {
        return QdrantEmbeddingStore.builder()
                .host(host)
                .port(port)
                .collectionName(collectionName)
                .build();
    }
}
