package com.javagenai.kb.embedding;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import java.io.IOException;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs one ONNX sentence-embedding model: tokenize, forward pass, mean-pool,
 * L2-normalize. The model stays loaded for the lifetime of the application so
 * the first query does not pay session start-up cost.
 */
public final class OnnxSentenceEncoder implements AutoCloseable {

    private final HuggingFaceTokenizer tokenizer;
    private final OrtEnvironment environment;
    private final OrtSession.SessionOptions sessionOptions;
    private final OrtSession session;

    public OnnxSentenceEncoder(Path model, Path tokenizerFile) throws IOException {
        try {
            tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile);
            environment = OrtEnvironment.getEnvironment();
            sessionOptions = new OrtSession.SessionOptions();
            session = environment.createSession(model.toString(), sessionOptions);
        } catch (Exception e) {
            throw new IOException("Could not open ONNX model " + model + ": " + e.getMessage(), e);
        }
    }

    /** Encodes one text into a normalized vector. */
    public float[] encode(String text) {
        Encoding encoding = tokenizer.encode(text);
        float[][][] hidden = lastHidden(encoding);
        float[] pooled = meanPool(hidden[0], encoding.getAttentionMask());
        l2Normalize(pooled);
        return pooled;
    }

    private float[][][] lastHidden(Encoding encoding) {
        try (OrtSession.Result result = run(encoding)) {
            Object output = result.get(0).getValue();
            if (output instanceof float[][][] hidden) {
                return hidden;
            }
            throw new IllegalStateException("unexpected model output " + output.getClass());
        } catch (Exception e) {
            throw new IllegalStateException("ONNX forward pass failed: " + e.getMessage(), e);
        }
    }

    /** Feeds inputs by name so models differing only in optional inputs still load. */
    private OrtSession.Result run(Encoding encoding) throws Exception {
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
            return session.run(feeds);
        } finally {
            for (OnnxTensor tensor : owned) {
                tensor.close();
            }
        }
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

    private float[] meanPool(float[][] tokenVectors, long[] attentionMask) {
        int dimension = tokenVectors[0].length;
        float[] accumulator = new float[dimension];
        float count = 0;
        for (int i = 0; i < tokenVectors.length && i < attentionMask.length; i++) {
            if (attentionMask[i] == 0) {
                continue;
            }
            count++;
            for (int d = 0; d < dimension; d++) {
                accumulator[d] += tokenVectors[i][d];
            }
        }
        if (count > 0) {
            for (int d = 0; d < dimension; d++) {
                accumulator[d] /= count;
            }
        }
        return accumulator;
    }

    private void l2Normalize(float[] vector) {
        double magnitude = 0;
        for (float value : vector) {
            magnitude += value * value;
        }
        magnitude = Math.sqrt(magnitude);
        if (magnitude == 0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / magnitude);
        }
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