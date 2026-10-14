package com.javagenai.lab7.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lab")
public class LabProperties {

    private double similarityThreshold = 0.80;
    private int cacheCapacity = 128;
    private int dimensions = 256;
    private int maxTokens = 128;
    private String modelPath = "models/nomic-embed-text-v1.5/model_quantized.onnx";
    private String tokenizerPath = "models/nomic-embed-text-v1.5/tokenizer.json";

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public int getCacheCapacity() {
        return cacheCapacity;
    }

    public void setCacheCapacity(int cacheCapacity) {
        this.cacheCapacity = cacheCapacity;
    }

    public int getDimensions() {
        return dimensions;
    }

    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getTokenizerPath() {
        return tokenizerPath;
    }

    public void setTokenizerPath(String tokenizerPath) {
        this.tokenizerPath = tokenizerPath;
    }
}
