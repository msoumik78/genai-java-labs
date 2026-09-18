package com.javagenai.lab1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lab")
public record LabPropertiesConfig(
        long extraHoldMs,
        int fenceSize
) {
    public LabPropertiesConfig {
        if (extraHoldMs < 0) {
            extraHoldMs = 0;
        }
        if (fenceSize < 1) {
            fenceSize = 2;
        }
    }
}
