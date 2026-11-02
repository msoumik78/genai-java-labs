package com.javagenai.rag.answer;

import java.util.List;

import com.javagenai.rag.retrieval.RankedPassage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class GroundedAnswerer {

    private static final String SYSTEM_PROMPT = """
            You answer questions about Northwind Field Service policy.
            The passages below are the only source you may use.
            When a passage states the fact, answer in one or two sentences and cite the passage id.
            If coverage is partial, state the condition. Do not answer "no" when the passage grants coverage for a limited time.
            Say you do not know only when none of the passages mention the subject of the question.
            A passage that ends mid-sentence still counts; use the sentences that are complete.
            """;

    private final ChatClient chatClient;

    public GroundedAnswerer(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String compose(String question, List<RankedPassage> passages) {
        if (passages.isEmpty()) {
            return "No passages were retrieved for that question.";
        }
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage(question, passages))
                .call()
                .content();
    }

    private static String userMessage(String question, List<RankedPassage> passages) {
        StringBuilder message = new StringBuilder();
        message.append("Question: ").append(question).append("\n\nPassages:\n");
        for (RankedPassage ranked : passages) {
            message.append("[").append(ranked.passage().passageId()).append("] ");
            message.append(ranked.passage().text()).append("\n\n");
        }
        return message.toString();
    }
}
