package com.javagenai.rag.rerank;

import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.javagenai.rag.retrieval.RankedPassage;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class CrossEncoderReranker {

    private static final int MAX_TOKENS = 256;

    private final HuggingFaceTokenizer tokenizer;
    private final OrtEnvironment environment;
    private final OrtSession session;

    public CrossEncoderReranker(com.javagenai.rag.config.RagSettings settings) {
        Path directory = Path.of(settings.crossEncoderDirectory());
        Path tokenizerPath = directory.resolve("tokenizer.json");
        Path modelPath = directory.resolve("model.onnx");
        if (!Files.isRegularFile(tokenizerPath) || !Files.isRegularFile(modelPath)) {
            throw new IllegalStateException("""
                    Cross-encoder files are missing under %s.
                    Expected tokenizer.json and model.onnx.
                    Run: scripts/download-cross-encoder.sh
                    """.formatted(directory.toAbsolutePath()));
        }
        try {
            this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath, Map.of(
                    "truncation", "true",
                    "maxLength", Integer.toString(MAX_TOKENS),
                    "padding", "false"));
            this.environment = OrtEnvironment.getEnvironment();
            this.session = environment.createSession(modelPath.toString(), new OrtSession.SessionOptions());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load the cross-encoder from " + directory.toAbsolutePath(), exception);
        }
    }

    public List<RankedPassage> rerank(String question, List<RankedPassage> fusedHits) {
        List<RankedPassage> reranked = new ArrayList<>();
        for (RankedPassage hit : fusedHits) {
            float relevanceLogit = relevanceLogit(question, hit.passage().text());
            reranked.add(new RankedPassage(hit.passage(), relevanceLogit));
        }
        reranked.sort(Comparator.comparingDouble(RankedPassage::score).reversed());
        return List.copyOf(reranked);
    }

    private float relevanceLogit(String question, String passageText) {
        Encoding encoding = tokenizer.encode(question, passageText);
        long[][] inputIds = new long[][] {encoding.getIds()};
        long[][] attentionMask = new long[][] {encoding.getAttentionMask()};
        long[][] tokenTypeIds = new long[][] {encoding.getTypeIds()};
        try (OnnxTensor inputIdTensor = OnnxTensor.createTensor(environment, inputIds);
                OnnxTensor attentionTensor = OnnxTensor.createTensor(environment, attentionMask);
                OnnxTensor tokenTypeTensor = OnnxTensor.createTensor(environment, tokenTypeIds);
                OrtSession.Result result = session.run(Map.of(
                        "input_ids", inputIdTensor,
                        "attention_mask", attentionTensor,
                        "token_type_ids", tokenTypeTensor))) {
            OnnxTensor logits = (OnnxTensor) result.get(0);
            FloatBuffer buffer = logits.getFloatBuffer();
            if (!buffer.hasRemaining()) {
                throw new IllegalStateException("Cross-encoder returned an empty logit");
            }
            float lastLogit = buffer.get(buffer.limit() - 1);
            return lastLogit;
        } catch (OrtException exception) {
            throw new IllegalStateException("Cross-encoder scoring failed", exception);
        }
    }

    @PreDestroy
    void close() {
        try {
            session.close();
            tokenizer.close();
        } catch (OrtException exception) {
            throw new IllegalStateException("Could not close the cross-encoder", exception);
        }
    }
}
