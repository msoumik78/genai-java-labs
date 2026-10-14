package com.javagenai.kb.generation;

import com.javagenai.kb.retrieval.RetrievalResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Answers from the retrieved chunks only. Empty context never reaches the model:
 * there is nothing to cite, so the honest result is an abstention.
 */
@ApplicationScoped
public class GroundedGenerator {

    static final String ABSTAIN = "ABSTAIN";

    private static final String SYSTEM = """
            Answer the question using only the numbered context.
            Cite the source of every factual sentence with its number in brackets, like [1].
            If the context does not contain the answer, reply with exactly ABSTAIN and nothing else.
            """;

    private final ChatClient chat;
    private final String model;
    private final double temperature;

    @Inject
    public GroundedGenerator(
            ChatClient chat,
            @ConfigProperty(name = "kb.generation.model", defaultValue = "deepseek-v4-pro") String model,
            @ConfigProperty(name = "kb.generation.temperature", defaultValue = "0.0") double temperature) {
        this.chat = chat;
        this.model = model;
        this.temperature = temperature;
    }

    public GeneratedAnswer answer(String question, List<RetrievalResult> retrieved) {
        List<RetrievalResult> context = retrieved == null ? List.of() : retrieved;
        if (context.isEmpty()) {
            return new GeneratedAnswer(ABSTAIN, true, List.of(), true, 0, 0, 0);
        }
        long started = System.nanoTime();
        ChatReply reply = chat.complete(model, temperature, SYSTEM, prompt(question, context));
        long latencyMs = (System.nanoTime() - started) / 1_000_000;
        String text = reply.text().strip();
        if (isAbstention(text)) {
            return new GeneratedAnswer(ABSTAIN, true, List.of(), true, reply.promptTokens(), reply.completionTokens(), latencyMs);
        }
        CitationCheck.Result citations = CitationCheck.check(text, context.stream()
                .map(result -> result.chunk().id())
                .toList());
        return new GeneratedAnswer(
                text,
                false,
                citations.chunkIds(),
                citations.valid(),
                reply.promptTokens(),
                reply.completionTokens(),
                latencyMs);
    }

    private static boolean isAbstention(String text) {
        String first = text.lines().findFirst().orElse("").strip();
        return first.equalsIgnoreCase(ABSTAIN);
    }

    private static String prompt(String question, List<RetrievalResult> context) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < context.size(); i++) {
            body.append('[').append(i + 1).append("] ").append(context.get(i).chunk().id()).append('\n');
            body.append(context.get(i).chunk().text()).append("\n\n");
        }
        body.append("Question: ").append(question);
        return body.toString();
    }
}
