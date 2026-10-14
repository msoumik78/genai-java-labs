package com.javagenai.lab7.embedding;

import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.javagenai.lab7.config.LabProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class OnnxNomicEmbedder {

    static final String QUERY_PREFIX = "search_query: ";

    private final int dimensions;
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;
    private final String outputName;

    public OnnxNomicEmbedder(LabProperties lab) {
        this.dimensions = lab.getDimensions();
        Path model = Path.of(lab.getModelPath());
        Path tokenizerPath = Path.of(lab.getTokenizerPath());
        if (!model.toFile().isFile() || !tokenizerPath.toFile().isFile()) {
            throw new IllegalStateException("""
                    Local embedding model is missing.
                    Run: ./scripts/download-model.sh
                    Expected:
                      %s
                      %s
                    """.formatted(model.toAbsolutePath(), tokenizerPath.toAbsolutePath()));
        }
        try {
            this.environment = OrtEnvironment.getEnvironment();
            this.session = environment.createSession(model.toString(), new OrtSession.SessionOptions());
            this.tokenizer = HuggingFaceTokenizer.builder()
                    .optTokenizerPath(tokenizerPath)
                    .optMaxLength(lab.getMaxTokens())
                    .optPadding(false)
                    .optTruncation(true)
                    .build();
            this.outputName = session.getOutputNames().contains("last_hidden_state")
                    ? "last_hidden_state"
                    : session.getOutputNames().iterator().next();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load the ONNX embedding model", exception);
        }
    }

    public float[] embed(String text) {
        Encoding encoding = tokenizer.encode(QUERY_PREFIX + text);
        long[] ids = encoding.getIds();
        long[] mask = encoding.getAttentionMask();
        long[] typeIds = encoding.getTypeIds();
        long[] shape = {1, ids.length};
        Set<String> inputs = session.getInputNames();
        try (OnnxTensor inputIds = tensor(ids, shape);
             OnnxTensor attentionMask = tensor(mask, shape);
             OnnxTensor tokenTypes = tensor(typeIds == null ? new long[ids.length] : typeIds, shape);
             OrtSession.Result result = session.run(namedInputs(inputs, inputIds, attentionMask, tokenTypes))) {
            OnnxTensor hidden = (OnnxTensor) result.get(outputName).orElseThrow();
            long[] outShape = hidden.getInfo().getShape();
            int hiddenSize = (int) outShape[outShape.length - 1];
            FloatBuffer buffer = hidden.getFloatBuffer();
            float[] tokenVectors = new float[buffer.remaining()];
            buffer.get(tokenVectors);
            float[] pooled = Vectors.meanPool(tokenVectors, mask, hiddenSize);
            Vectors.normalize(pooled);
            return Vectors.truncateAndNormalize(pooled, dimensions);
        } catch (Exception exception) {
            throw new IllegalStateException("Embedding failed", exception);
        }
    }

    private OnnxTensor tensor(long[] values, long[] shape) throws ai.onnxruntime.OrtException {
        return OnnxTensor.createTensor(environment, LongBuffer.wrap(values), shape);
    }

    private static Map<String, OnnxTensor> namedInputs(Set<String> inputs, OnnxTensor ids, OnnxTensor mask, OnnxTensor types) {
        Map<String, OnnxTensor> named = new java.util.LinkedHashMap<>();
        if (inputs.contains("input_ids")) {
            named.put("input_ids", ids);
        }
        if (inputs.contains("attention_mask")) {
            named.put("attention_mask", mask);
        }
        if (inputs.contains("token_type_ids")) {
            named.put("token_type_ids", types);
        }
        return named;
    }

    @PreDestroy
    void close() throws Exception {
        session.close();
        tokenizer.close();
    }
}
