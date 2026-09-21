package com.javagenai.lab3.djl;

import java.util.Arrays;

import ai.djl.Application;
import ai.djl.Device;
import ai.djl.huggingface.translator.FillMaskTranslatorFactory;
import ai.djl.huggingface.translator.QuestionAnsweringTranslatorFactory;
import ai.djl.huggingface.translator.TextClassificationTranslatorFactory;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.huggingface.translator.TokenClassificationTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.nlp.qa.QAInput;
import ai.djl.modality.nlp.translator.NamedEntity;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslatorFactory;
import com.javagenai.lab3.Cli;

/**
 * Path 1 — Hub weights via DJL + PyTorch (MPS on Apple Silicon).
 * First arg is the pipeline name; remaining args are the text.
 */
public final class DjlPytorchMpsMain {

    public static void main(String[] args) throws Exception {
        Cli cli = Cli.parse(args);
        if ("help".equals(cli.task())) {
            System.out.print(Cli.usage());
            return;
        }
        Device device = pickDevice();
        System.out.println("path     DJL + PyTorch");
        System.out.println("device   " + device);
        System.out.println("task     " + cli.task());
        try {
            run(device, cli);
        } catch (Throwable mpsFailed) {
            if (!String.valueOf(device).toLowerCase().contains("mps")) {
                throw mpsFailed;
            }
            System.out.println("MPS failed (" + mpsFailed.getMessage() + ") — retry CPU");
            run(Device.cpu(), cli);
        }
    }

    private static void run(Device device, Cli cli) throws Exception {
        switch (cli.task()) {
            case "sentiment" -> classify(device, cli.text());
            case "ner" -> ner(device, cli.text());
            case "qa" -> qa(device, cli.text(), cli.context());
            case "embed" -> embed(device, cli.text());
            case "fill-mask" -> fillMask(device, cli.text());
            default -> {
                System.out.print(Cli.usage());
                throw new IllegalArgumentException("unknown task " + cli.task());
            }
        }
    }

    private static void classify(Device device, String text) throws Exception {
        var out = predict(
                device,
                Application.NLP.TEXT_CLASSIFICATION,
                "djl://ai.djl.huggingface.pytorch/distilbert-base-uncased-finetuned-sst-2-english",
                String.class,
                Classifications.class,
                new TextClassificationTranslatorFactory(),
                text);
        System.out.println("result   " + out);
    }

    private static void ner(Device device, String text) throws Exception {
        NamedEntity[] out = predict(
                device,
                Application.NLP.TOKEN_CLASSIFICATION,
                "djl://ai.djl.huggingface.pytorch/dslim/bert-base-NER",
                String.class,
                NamedEntity[].class,
                new TokenClassificationTranslatorFactory(),
                text);
        System.out.println("result   " + Arrays.toString(out));
    }

    private static void qa(Device device, String question, String context) throws Exception {
        String out = predict(
                device,
                Application.NLP.QUESTION_ANSWER,
                "djl://ai.djl.huggingface.pytorch/distilbert-base-cased-distilled-squad",
                QAInput.class,
                String.class,
                new QuestionAnsweringTranslatorFactory(),
                new QAInput(question, context));
        System.out.println("question " + question);
        System.out.println("context  " + context);
        System.out.println("result   " + out);
    }

    private static void embed(Device device, String text) throws Exception {
        float[] v = predict(
                device,
                Application.NLP.TEXT_EMBEDDING,
                "djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2",
                String.class,
                float[].class,
                new TextEmbeddingTranslatorFactory(),
                text);
        System.out.println("result   dim=" + v.length + "  head=" + Arrays.toString(Arrays.copyOf(v, Math.min(8, v.length))));
    }

    private static void fillMask(Device device, String text) throws Exception {
        var out = predict(
                device,
                Application.NLP.FILL_MASK,
                "djl://ai.djl.huggingface.pytorch/bert-base-uncased",
                String.class,
                Classifications.class,
                new FillMaskTranslatorFactory(),
                text);
        System.out.println("result   " + out);
    }

    private static <I, O> O predict(
            Device device,
            Application application,
            String modelUrl,
            Class<I> in,
            Class<O> out,
            TranslatorFactory factory,
            I input)
            throws Exception {
        System.out.println("model    " + modelUrl);
        System.out.println("input    " + input);
        Criteria<I, O> criteria = Criteria.builder()
                .optApplication(application)
                .setTypes(in, out)
                .optModelUrls(modelUrl)
                .optEngine("PyTorch")
                .optDevice(device)
                .optTranslatorFactory(factory)
                .optProgress(new ProgressBar())
                .build();
        try (ZooModel<I, O> model = criteria.loadModel();
                Predictor<I, O> predictor = model.newPredictor()) {
            return predictor.predict(input);
        }
    }

    static Device pickDevice() {
        String forced = System.getProperty("lab3.device", "auto");
        if ("cpu".equalsIgnoreCase(forced)) {
            return Device.cpu();
        }
        if ("mps".equalsIgnoreCase(forced)) {
            return Device.fromName("mps");
        }
        String os = System.getProperty("os.name", "");
        String arch = System.getProperty("os.arch", "");
        if (os.toLowerCase().contains("mac") && arch.contains("aarch64")) {
            return Device.fromName("mps");
        }
        return Device.cpu();
    }
}
