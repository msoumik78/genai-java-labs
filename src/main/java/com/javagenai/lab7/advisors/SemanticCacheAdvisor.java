package com.javagenai.lab7.advisors;

import java.util.List;
import java.util.Map;

import com.javagenai.lab7.cache.SemanticCache;
import com.javagenai.lab7.embedding.OnnxNomicEmbedder;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class SemanticCacheAdvisor implements CallAdvisor {

    private final OnnxNomicEmbedder embedder;
    private final SemanticCache cache;

    public SemanticCacheAdvisor(OnnxNomicEmbedder embedder, SemanticCache cache) {
        this.embedder = embedder;
        this.cache = cache;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String question = request.prompt().getUserMessage().getText();
        long started = System.nanoTime();
        float[] vector = embedder.embed(question);
        long embeddingMillis = (System.nanoTime() - started) / 1_000_000;

        SemanticCache.Match match = cache.search(vector);
        if (match.hit()) {
            ChatResponse cached = new ChatResponse(List.of(new Generation(new AssistantMessage(match.answer()))));
            return ChatClientResponse.builder()
                    .chatResponse(cached)
                    .context(request.context())
                    .context("cacheHit", true)
                    .context("score", match.score())
                    .context("embeddingMillis", embeddingMillis)
                    .build();
        }

        ChatClientResponse response = chain.nextCall(request);
        String answer = response.chatResponse().getResult().getOutput().getText();
        cache.store(vector, answer);
        Map<String, Object> context = new java.util.LinkedHashMap<>(response.context());
        context.put("cacheHit", false);
        context.put("score", match.score());
        context.put("embeddingMillis", embeddingMillis);
        return ChatClientResponse.builder()
                .chatResponse(response.chatResponse())
                .context(context)
                .build();
    }

    @Override
    public String getName() {
        return "semantic-cache";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
