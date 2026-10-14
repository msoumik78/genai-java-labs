package com.javagenai.lab7.config;

import com.javagenai.lab7.advisors.SemanticCacheAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, SemanticCacheAdvisor semanticCacheAdvisor) {
        return builder.defaultAdvisors(semanticCacheAdvisor).build();
    }
}
