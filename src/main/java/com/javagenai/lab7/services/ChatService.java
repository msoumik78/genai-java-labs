package com.javagenai.lab7.services;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Map<String, Object> chat(String question) {
        long started = System.nanoTime();
        ChatClientResponse response = chatClient.prompt().user(question).call().chatClientResponse();
        long totalMillis = (System.nanoTime() - started) / 1_000_000;
        Map<String, Object> context = response.context();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("answer", response.chatResponse().getResult().getOutput().getText());
        body.put("cacheHit", context.get("cacheHit"));
        body.put("score", context.get("score"));
        body.put("embeddingMillis", context.get("embeddingMillis"));
        body.put("totalMillis", totalMillis);
        return body;
    }
}
