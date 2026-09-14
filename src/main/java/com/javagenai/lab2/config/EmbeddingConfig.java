package com.javagenai.lab2.config;

import java.util.Map;

import com.javagenai.lab2.kb.TextEmbedder;
import com.javagenai.lab2.kb.TransformerEmbedder;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    /**
     * Spring AI's transformers auto-config is easy to skip (DJL tokenizer
     * {@code @ConditionalOnClass}, {@code spring.ai.model.embedding}). Build the
     * ONNX model here so a missing embedder is a real load error, not a silent skip.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.embedding", havingValue = "transformers", matchIfMissing = true)
    @ConditionalOnMissingBean(TextEmbedder.class)
    TextEmbedder textEmbedder(
            LabProperties lab,
            @Value("${spring.ai.embedding.transformer.onnx.model-uri}") String modelUri,
            @Value("${spring.ai.embedding.transformer.tokenizer.uri}") String tokenizerUri)
            throws Exception {
        TransformersEmbeddingModel model = new TransformersEmbeddingModel();
        model.setModelResource(modelUri);
        model.setTokenizerResource(tokenizerUri);
        model.setResourceCacheDirectory(System.getProperty("user.home") + "/.cache/lab2-onnx");
        model.setTokenizerOptions(Map.of("padding", "true"));
        model.afterPropertiesSet();
        return new TransformerEmbedder(model, lab);
    }
}
