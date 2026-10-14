package com.javagenai.kb.judge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javagenai.kb.generation.ChatClient;
import com.javagenai.kb.generation.ChatReply;
import com.javagenai.kb.retrieval.RetrievalResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Grades an answer against the context the generator saw. The model id is
 * separate from the generator so the judge is not grading its own homework.
 */
@ApplicationScoped
public class FaithfulnessJudge {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM = """
            You judge an answer against the context it was given. Reply with JSON only:
            {"claims":[{"text":"...","verdict":"SUPPORTED"}],"relevancy":0.0,"completeness":0.0}
            Split the answer into claims. Each verdict is SUPPORTED, UNSUPPORTED, or CONTRADICTED.
            relevancy: does the answer address the question, from 0 to 1.
            completeness: does it cover every part of the question, from 0 to 1.
            """;

    private final ChatClient chat;
    private final String model;
    private final double temperature;

    @Inject
    public FaithfulnessJudge(
            ChatClient chat,
            @ConfigProperty(name = "kb.judge.model", defaultValue = "deepseek-flash") String model,
            @ConfigProperty(name = "kb.judge.temperature", defaultValue = "0.0") double temperature) {
        this.chat = chat;
        this.model = model;
        this.temperature = temperature;
    }

    /** Abstentions are not graded. A refusal has no claims to support. */
    public Judgement judge(String question, String answer, boolean abstained, List<RetrievalResult> context) {
        if (abstained) {
            return new Judgement(null, null, null, List.of(), model);
        }
        ChatReply reply = chat.complete(model, temperature, SYSTEM, prompt(question, answer, context));
        return parse(reply.text());
    }

    public String model() {
        return model;
    }

    private Judgement parse(String text) {
        try {
            JsonNode root = JSON.readTree(jsonObject(text));
            List<Judgement.Claim> claims = new ArrayList<>();
            for (JsonNode claim : root.path("claims")) {
                claims.add(new Judgement.Claim(
                        claim.path("text").asText(""),
                        claim.path("verdict").asText("UNSUPPORTED")));
            }
            double supported = claims.stream().filter(claim -> "SUPPORTED".equals(claim.verdict())).count();
            Double faithfulness = claims.isEmpty() ? null : supported / claims.size();
            return new Judgement(
                    faithfulness,
                    score(root, "relevancy"),
                    score(root, "completeness"),
                    claims,
                    model);
        } catch (Exception e) {
            throw new IllegalStateException("Judge returned unreadable JSON", e);
        }
    }

    private static Double score(JsonNode root, String field) {
        JsonNode node = root.path(field);
        return node.isNumber() ? node.asDouble() : null;
    }

    /** Models sometimes wrap JSON in a fence. The object is what we asked for. */
    private static String jsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return text;
        }
        return text.substring(start, end + 1);
    }

    private static String prompt(String question, String answer, List<RetrievalResult> context) {
        StringBuilder body = new StringBuilder();
        body.append("Context:\n");
        if (context != null) {
            for (int i = 0; i < context.size(); i++) {
                body.append('[').append(i + 1).append("] ").append(context.get(i).chunk().text()).append("\n\n");
            }
        }
        body.append("Question: ").append(question).append("\n");
        body.append("Answer: ").append(answer);
        return body.toString();
    }
}
