package com.javagenai.ingest.store;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

public final class ChromaStore implements EmbeddingStoreStrategy {

    private final String baseUrl;
    private final String collectionName;

    public ChromaStore(String baseUrl, String collectionName) {
        this.baseUrl = baseUrl;
        this.collectionName = collectionName;
    }

    @Override
    public String name() {
        return "chroma";
    }

    @Override
    public EmbeddingStore<TextSegment> open() {
        return ChromaEmbeddingStore.builder()
                .apiVersion(ChromaApiVersion.V2)
                .baseUrl(baseUrl)
                .tenantName("default_tenant")
                .databaseName("default_database")
                .collectionName(collectionName)
                .build();
    }
}
