package com.javagenai.lab6.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.Prompt;

class TokenUsageHandlerTest {

    @AfterEach
    void clearTenant() {
        TenantHolder.clear();
    }

    @Test
    void recordsInputAndOutputTokensForTheTenant() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TokenUsageHandler handler = new TokenUsageHandler(registry);

        ChatResponse response = new ChatResponse(
                List.of(new Generation(new org.springframework.ai.chat.messages.AssistantMessage("hi"))),
                ChatResponseMetadata.builder()
                        .model("llama3.2:1b")
                        .usage(new DefaultUsage(11, 4))
                        .build());

        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt("hello"))
                .provider("ollama")
                .build();
        context.setResponse(response);

        TenantHolder.set("acme");
        handler.onStop(context);

        assertThat(registry.get(TokenUsageHandler.TOKENS)
                .tag("tenant", "acme")
                .tag("token_type", "input")
                .counter().count()).isEqualTo(11);
        assertThat(registry.get(TokenUsageHandler.TOKENS)
                .tag("tenant", "acme")
                .tag("token_type", "output")
                .counter().count()).isEqualTo(4);
        assertThat(registry.get(TokenUsageHandler.CALLS)
                .tag("tenant", "acme")
                .counter().count()).isEqualTo(1);
    }
}
