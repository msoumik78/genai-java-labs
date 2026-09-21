package com.javagenai.lab3.onnx;

import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;
import ai.onnxruntime.OrtSession.SessionOptions;
import com.javagenai.lab3.Cli;
import com.javagenai.lab3.HubCache;

/**
 * Path 2 — exported ONNX + tokenizer, CoreML when it loads.
 * Same task switch as {@link com.javagenai.lab3.djl.DjlPytorchMpsMain}.
 */
public final class OnnxCoreMlMain {

    private static final String[] SST2 = {"NEGATIVE", "POSITIVE"};
    private static final String[] NER = {
            "O", "B-MISC", "I-MISC", "B-PER", "I-PER", "B-ORG", "I-ORG", "B-LOC", "I-LOC"
    };

    public static void main(String[] args) throws Exception {
        Cli cli = Cli.parse(args);
        if ("help".equals(cli.task())) {
            System.out.print(Cli.usage());
            return;
        }
        System.out.println("path     ONNX Runtime");
        System.out.println("task     " + cli.task());
        switch (cli.task()) {
            case "sentiment" -> sentiment(cli.text());
            case "ner" -> ner(cli.text());
            case "qa" -> qa(cli.text(), cli.context());
            case "embed" -> embed(cli.text());
            case "fill-mask" -> {
                System.out.println("ONNX fill-mask needs an MLM export + decode loop.");
                System.out.println("Use DjlPytorchMpsMain fill-mask for that pipeline.");
            }
            default -> {
                System.out.print(Cli.usage());
                throw new IllegalArgumentException("unknown task " + cli.task());
            }
        }
    }

    private static void sentiment(String text) throws Exception {
        Path onnx = HubCache.download(
                "https://huggingface.co/Xenova/distilbert-base-uncased-finetuned-sst-2-english/resolve/main/onnx/model.onnx",
                "sst2-distilbert.onnx");
        Path tok = HubCache.download(
                "https://huggingface.co/Xenova/distilbert-base-uncased-finetuned-sst-2-english/resolve/main/tokenizer.json",
                "sst2-distilbert-tokenizer.json");
        try (Encoder enc = Encoder.open(onnx, tok)) {
            float[] logits = enc.logits2(text);
            int best = argmax(logits);
            float[] p = softmax(logits);
            System.out.println("text     " + text);
            System.out.println("result   " + SST2[best] + "  p=" + String.format("%.4f", p[best]));
        }
    }

    private static void ner(String text) throws Exception {
        Path onnx = HubCache.download(
                "https://huggingface.co/Xenova/bert-base-NER/resolve/main/onnx/model.onnx",
                "bert-base-ner.onnx");
        Path tok = HubCache.download(
                "https://huggingface.co/Xenova/bert-base-NER/resolve/main/tokenizer.json",
                "bert-base-ner-tokenizer.json");
        try (Encoder enc = Encoder.open(onnx, tok)) {
            Encoding encoding = enc.tokenizer.encode(text);
            float[][][] logits = enc.tokenLogits(encoding);
            List<String> hits = new ArrayList<>();
            String[] tokens = encoding.getTokens();
            for (int i = 0; i < tokens.length && i < logits[0].length; i++) {
                int lab = argmax(logits[0][i]);
                if (lab > 0 && lab < NER.length && !tokens[i].startsWith("[")) {
                    hits.add(tokens[i] + "=" + NER[lab]);
                }
            }
            System.out.println("text     " + text);
            System.out.println("result   " + hits);
        }
    }

    private static void qa(String question, String context) throws Exception {
        Path onnx = HubCache.download(
                "https://huggingface.co/Xenova/distilbert-base-cased-distilled-squad/resolve/main/onnx/model.onnx",
                "squad-distilbert.onnx");
        Path tok = HubCache.download(
                "https://huggingface.co/Xenova/distilbert-base-cased-distilled-squad/resolve/main/tokenizer.json",
                "squad-distilbert-tokenizer.json");
        try (Encoder enc = Encoder.open(onnx, tok)) {
            Encoding encoding = enc.tokenizer.encode(question, context);
            ResultPack pack = enc.run(encoding);
            float[] start = pack.named("start_logits");
            float[] end = pack.named("end_logits");
            if (start == null) {
                start = pack.firstRow();
                end = pack.secondRow();
            }
            int s = argmax(start);
            int e = argmax(end);
            if (e < s) {
                e = s;
            }
            long[] ids = Arrays.copyOfRange(encoding.getIds(), s, Math.min(e + 1, encoding.getIds().length));
            String answer = enc.tokenizer.decode(ids);
            System.out.println("question " + question);
            System.out.println("context  " + context);
            System.out.println("result   " + answer);
        }
    }

    private static void embed(String text) throws Exception {
        Path onnx = HubCache.download(
                "https://huggingface.co/Xenova/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx",
                "minilm.onnx");
        Path tok = HubCache.download(
                "https://huggingface.co/Xenova/all-MiniLM-L6-v2/resolve/main/tokenizer.json",
                "minilm-tokenizer.json");
        try (Encoder enc = Encoder.open(onnx, tok)) {
            Encoding encoding = enc.tokenizer.encode(text);
            float[][][] hidden = enc.lastHidden(encoding);
            float[] v = meanPool(hidden[0], encoding.getAttentionMask());
            l2(v);
            System.out.println("text     " + text);
            System.out.println("result   dim=" + v.length + "  head=" + Arrays.toString(Arrays.copyOf(v, Math.min(8, v.length))));
        }
    }

    private static final class Encoder implements AutoCloseable {
        final HuggingFaceTokenizer tokenizer;
        final OrtEnvironment env;
        final SessionOptions opts;
        final OrtSession session;

        static Encoder open(Path onnx, Path tok) throws Exception {
            return new Encoder(onnx, tok);
        }

        private Encoder(Path onnx, Path tok) throws Exception {
            tokenizer = HuggingFaceTokenizer.newInstance(tok);
            env = OrtEnvironment.getEnvironment();
            opts = sessionOptions();
            session = env.createSession(onnx.toString(), opts);
            System.out.println("inputs   " + session.getInputNames());
        }

        float[] logits2(String text) throws Exception {
            float[][] row = (float[][]) run(tokenizer.encode(text)).raw(0);
            return row[0];
        }

        float[][][] tokenLogits(Encoding encoding) throws Exception {
            return (float[][][]) run(encoding).raw(0);
        }

        float[][][] lastHidden(Encoding encoding) throws Exception {
            Object v = run(encoding).raw(0);
            if (v instanceof float[][][] f) {
                return f;
            }
            throw new IllegalStateException("unexpected embedding output " + v.getClass());
        }

        ResultPack run(Encoding encoding) throws Exception {
            long[] ids = encoding.getIds();
            long[] mask = encoding.getAttentionMask();
            long[] shape = {1, ids.length};
            Map<String, OnnxTensor> feeds = new LinkedHashMap<>();
            List<OnnxTensor> owned = new ArrayList<>();
            try {
                for (String name : session.getInputNames()) {
                    OnnxTensor t = switch (name) {
                        case "input_ids" -> OnnxTensor.createTensor(env, LongBuffer.wrap(ids), shape);
                        case "attention_mask" -> OnnxTensor.createTensor(env, LongBuffer.wrap(mask), shape);
                        case "token_type_ids" -> OnnxTensor.createTensor(env, LongBuffer.wrap(new long[ids.length]), shape);
                        default -> throw new IllegalStateException("unexpected input " + name);
                    };
                    owned.add(t);
                    feeds.put(name, t);
                }
                Result result = session.run(feeds);
                return new ResultPack(result);
            } finally {
                for (OnnxTensor t : owned) {
                    t.close();
                }
            }
        }

        @Override
        public void close() {
            try {
                session.close();
            } catch (Exception ignored) {
            }
            opts.close();
            tokenizer.close();
        }
    }

    private static final class ResultPack implements AutoCloseable {
        final Result result;

        ResultPack(Result result) {
            this.result = result;
        }

        Object raw(int i) throws Exception {
            return result.get(i).getValue();
        }

        float[] named(String name) throws Exception {
            try {
                OnnxValue v = result.get(name).orElse(null);
                if (v == null) {
                    return null;
                }
                Object o = v.getValue();
                if (o instanceof float[][] row) {
                    return row[0];
                }
                if (o instanceof float[] f) {
                    return f;
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }

        float[] firstRow() throws Exception {
            return ((float[][]) raw(0))[0];
        }

        float[] secondRow() throws Exception {
            return ((float[][]) raw(1))[0];
        }

        @Override
        public void close() {
            result.close();
        }
    }

    private static SessionOptions sessionOptions() {
        SessionOptions opts = new SessionOptions();
        try {
            opts.addCoreML();
            System.out.println("ep       CoreML");
        } catch (Exception ex) {
            System.out.println("ep       CPU (" + ex.getMessage() + ")");
        }
        return opts;
    }

    private static float[] meanPool(float[][] tokens, long[] mask) {
        int dim = tokens[0].length;
        float[] acc = new float[dim];
        float n = 0;
        for (int i = 0; i < tokens.length && i < mask.length; i++) {
            if (mask[i] == 0) {
                continue;
            }
            n += 1;
            for (int d = 0; d < dim; d++) {
                acc[d] += tokens[i][d];
            }
        }
        if (n > 0) {
            for (int d = 0; d < dim; d++) {
                acc[d] /= n;
            }
        }
        return acc;
    }

    private static void l2(float[] v) {
        float s = 0;
        for (float x : v) {
            s += x * x;
        }
        s = (float) Math.sqrt(s);
        if (s == 0) {
            return;
        }
        for (int i = 0; i < v.length; i++) {
            v[i] /= s;
        }
    }

    private static int argmax(float[] v) {
        int i = 0;
        for (int j = 1; j < v.length; j++) {
            if (v[j] > v[i]) {
                i = j;
            }
        }
        return i;
    }

    private static float[] softmax(float[] logits) {
        float max = logits[0];
        for (float x : logits) {
            max = Math.max(max, x);
        }
        float[] out = new float[logits.length];
        float sum = 0;
        for (int i = 0; i < logits.length; i++) {
            out[i] = (float) Math.exp(logits[i] - max);
            sum += out[i];
        }
        for (int i = 0; i < out.length; i++) {
            out[i] /= sum;
        }
        return out;
    }
}
