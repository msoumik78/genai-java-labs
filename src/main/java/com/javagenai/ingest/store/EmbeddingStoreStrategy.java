package com.javagenai.ingest.store;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public interface EmbeddingStoreStrategy {

    String name();

    EmbeddingStore<TextSegment> open();
}
