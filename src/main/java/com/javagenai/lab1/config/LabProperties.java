package com.javagenai.lab1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lab")
public record LabProperties(
        long extraHoldMs,
        int fenceSize
) {
    public LabProperties {
        if (extraHoldMs < 0) {
            extraHoldMs = 0;
        }
        if (fenceSize < 1) {
            fenceSize = 2;
        }
    }
}
