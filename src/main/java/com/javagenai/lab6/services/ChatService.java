package com.javagenai.lab6.services;

import java.util.LinkedHashMap;
import java.util.Map;

import com.javagenai.lab6.observability.TenantHolder;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatModel chatModel;

    public ChatService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public Map<String, Object> chat(String question, String tenant) {
        TenantHolder.set(tenant);
        try {
            ChatResponse response = chatModel.call(new Prompt(question));
            Usage usage = response.getMetadata().getUsage();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("tenant", tenant);
            body.put("model", response.getMetadata().getModel());
            body.put("answer", response.getResult().getOutput().getText());
            body.put("inputTokens", usage == null ? null : usage.getPromptTokens());
            body.put("outputTokens", usage == null ? null : usage.getCompletionTokens());
            body.put("totalTokens", usage == null ? null : usage.getTotalTokens());
            return body;
        } finally {
            TenantHolder.clear();
        }
    }
}
