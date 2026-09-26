package com.javagenai.ingest.store;

public final class Stores {

    private Stores() {
    }

    public static EmbeddingStoreStrategy select(String name) {
        return switch (name) {
            case "chroma" -> new ChromaStore(env("CHROMA_URL", "http://localhost:8000"), env("CHROMA_COLLECTION", "docs"));
            case "pinecone" -> new PineconeStore(required("PINECONE_API_KEY"), env("PINECONE_INDEX", "docs"));
            case "qdrant" -> new QdrantStore(env("QDRANT_HOST", "localhost"), Integer.parseInt(env("QDRANT_PORT", "6334")), env("QDRANT_COLLECTION", "docs"));
            default -> throw new IllegalArgumentException("Unknown store: " + name);
        };
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing " + key);
        }
        return value;
    }
}
