package com.javagenai.kb.retrieval;

import com.javagenai.kb.embedding.HubFileCache;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Reranking as a bean: lazy local model, disabled by one config flag so an
 * eval run can ablate it (the rerank step is the experiment, not a given).
 */
@ApplicationScoped
public class Reranker {

    private static final String ONNX_PATH = "onnx/model_quantized.onnx";

    @ConfigProperty(name = "kb.reranker.model-id", defaultValue = "Xenova/bge-reranker-base")
    String modelId;

    private volatile OnnxReranker model;

    /** Rescores a fused list by (query, chunk) relevance; the caller decides if this happens. */
    public List<RetrievalResult> rerank(String question, List<RetrievalResult> candidates) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        List<Scored> scored = new java.util.ArrayList<>();
        for (RetrievalResult result : candidates) {
            scored.add(new Scored(result, local().score(question, result.chunk().text())));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored.stream().map(Scored::result).toList();
    }

    private OnnxReranker local() {
        OnnxReranker local = model;
        if (local == null) {
            synchronized (this) {
                if (model == null) {
                    model = local = open();
                }
            }
        }
        return local;
    }

    private OnnxReranker open() {
        try {
            String base = "https://huggingface.co/" + modelId + "/resolve/main/";
            String stem = modelId.replace('/', '-');
            Path modelPath = HubFileCache.download(base + ONNX_PATH, stem + ".onnx");
            Path tokenizer = HubFileCache.download(base + "tokenizer.json",
                    stem + "-tokenizer.json");
            return new OnnxReranker(modelPath, tokenizer);
        } catch (IOException e) {
            throw new IllegalStateException("Reranker load failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Reranker download was interrupted", e);
        }
    }

    @PreDestroy
    void closeModel() {
        OnnxReranker local = model;
        if (local != null) {
            local.close();
        }
    }

    private record Scored(RetrievalResult result, double score) {
    }
}