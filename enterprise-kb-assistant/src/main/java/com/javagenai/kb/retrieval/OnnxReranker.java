package com.javagenai.kb.retrieval;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.javagenai.kb.embedding.HubFileCache;
import java.io.IOException;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cross-encoder reranking: one forward pass per (query, chunk) pair scores how
 * well they belong together, which is far more accurate than comparing two
 * vectors that were embedded independently. Costs CPU, costs no tokens.
 *
 * <p>Runs locally over an int8 ONNX export, so the demo's biggest faithfulness
 * lever is free and reproducible.
 */
public final class OnnxReranker implements AutoCloseable {

    private final HuggingFaceTokenizer tokenizer;
    private final OrtEnvironment environment;
    private final OrtSession.SessionOptions sessionOptions;
    private final OrtSession session;

    public OnnxReranker(Path model, Path tokenizerFile) throws IOException {
        try {
            tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile);
            environment = OrtEnvironment.getEnvironment();
            sessionOptions = new OrtSession.SessionOptions();
            session = environment.createSession(model.toString(), sessionOptions);
        } catch (Exception e) {
            throw new IOException("Could not open reranker model " + model + ": " + e.getMessage(), e);
        }
    }

    /** Scores one pair; higher means a better match. Sigmoid maps the logit to 0..1. */
    public double score(String query, String passage) {
        Encoding encoding = tokenizer.encode(query, passage);
        float logit = firstLogit(encoding);
        return 1.0 / (1.0 + Math.exp(-logit));
    }

    private float firstLogit(Encoding encoding) {
        try {
            long[] ids = encoding.getIds();
            long[] mask = encoding.getAttentionMask();
            long[] shape = {1, ids.length};
            Map<String, OnnxTensor> feeds = new LinkedHashMap<>();
            List<OnnxTensor> owned = new ArrayList<>();
            try {
                for (String name : session.getInputNames()) {
                    OnnxTensor tensor = inputFor(name, ids, mask, shape);
                    owned.add(tensor);
                    feeds.put(name, tensor);
                }
                try (OrtSession.Result result = session.run(feeds)) {
                    return relevanceLogit(result);
                }
            } finally {
                for (OnnxTensor tensor : owned) {
                    tensor.close();
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Reranker forward pass failed: " + e.getMessage(), e);
        }
    }

    /** Rerankers emit [1,1] or [1,sequence,labels]; the relevance logit is always first. */
    private float relevanceLogit(OrtSession.Result result) throws OrtException {
        Object output = result.get(0).getValue();
        if (output instanceof float[][][] batch) {
            return batch[0][0][0];
        }
        if (output instanceof float[][] row) {
            return row[0][0];
        }
        if (output instanceof float[] flat) {
            return flat[0];
        }
        throw new IllegalStateException("unexpected reranker output " + output.getClass());
    }

    private OnnxTensor inputFor(String name, long[] ids, long[] mask, long[] shape) throws OrtException {
        return switch (name) {
            case "input_ids" -> OnnxTensor.createTensor(environment, LongBuffer.wrap(ids), shape);
            case "attention_mask" -> OnnxTensor.createTensor(environment, LongBuffer.wrap(mask), shape);
            case "token_type_ids" -> OnnxTensor.createTensor(environment,
                    LongBuffer.wrap(new long[ids.length]), shape);
            default -> throw new IllegalStateException("unexpected model input " + name);
        };
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (Exception ignored) {
            // Session close failures are not actionable at shutdown.
        }
        try {
            sessionOptions.close();
        } catch (Exception ignored) {
            // Same.
        }
        tokenizer.close();
    }
}