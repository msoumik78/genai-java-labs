package com.javagenai.ingest.store;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;

public final class PineconeStore implements EmbeddingStoreStrategy {

    private final String apiKey;
    private final String indexName;

    public PineconeStore(String apiKey, String indexName) {
        this.apiKey = apiKey;
        this.indexName = indexName;
    }

    @Override
    public String name() {
        return "pinecone";
    }

    @Override
    public EmbeddingStore<TextSegment> open() {
        return PineconeEmbeddingStore.builder()
                .apiKey(apiKey)
                .index(indexName)
                .nameSpace("docs")
                .createIndex(PineconeServerlessIndexConfig.builder()
                        .cloud("AWS")
                        .region("us-east-1")
                        .dimension(384)
                        .build())
                .build();
    }
}
