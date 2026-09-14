package com.javagenai.lab2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "lab")
public record LabProperties(
        @DefaultValue("384") int embeddingDims,
        @DefaultValue("3") int topK,
        /** Cosine distance ceiling; 0 disables. Weak neighbors are dropped. */
        @DefaultValue("0.55") double maxDistance,
        @DefaultValue("bge-small-en-v1.5") String embedder,
        @DefaultValue("Represent this sentence for searching relevant passages: ") String embedQueryPrefix) {
}
