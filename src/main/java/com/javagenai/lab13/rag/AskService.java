package com.javagenai.lab13.rag;

import com.javagenai.lab13.retrieval.KeywordRetriever;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AskService {

    private final KeywordRetriever retriever;
    private final ChatClient chatClient;
    private final int topK;

    public AskService(
            KeywordRetriever retriever,
            ChatClient.Builder chatClientBuilder,
            @Value("${lab.top-k:2}") int topK) {
        this.retriever = retriever;
        this.chatClient = chatClientBuilder.build();
        this.topK = topK;
    }

    public Answer answer(String question) {
        List<String> contexts = retriever.retrieve(question, topK);
        String prompt = """
                Answer the question using only the passages below.
                If the passages do not contain the answer, say you do not know.

                Passages:
                %s

                Question: %s
                """.formatted(String.join("\n", contexts), question);
        String response = chatClient.prompt().user(prompt).call().content();
        return new Answer(question, contexts, response);
    }

    public record Answer(String question, List<String> contexts, String response) {
    }
}
