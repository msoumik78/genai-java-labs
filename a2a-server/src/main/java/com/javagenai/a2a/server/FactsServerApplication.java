package com.javagenai.a2a.server;

import java.util.List;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FactsServerApplication {

    private static final String SYSTEM = """
            You are the order-facts agent.
            Answer only by calling lookupOrder.
            Repeat the tool result. Do not invent order ids or statuses.
            """;

    public static void main(String[] args) {
        SpringApplication.run(FactsServerApplication.class, args);
    }

    @Bean
    public AgentCard agentCard(@Value("${server.port}") int port) {
        return new AgentCard.Builder()
                .name("Order Facts")
                .description("Looks up the status of a known order id")
                .url("http://127.0.0.1:" + port + "/")
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder().streaming(false).build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(new AgentSkill.Builder()
                        .id("order_facts")
                        .name("Order facts")
                        .description("Return status, city, and amount for an order id such as A-100")
                        .tags(List.of("orders"))
                        .examples(List.of("What is the status of order A-100?"))
                        .build()))
                .protocolVersion("0.3.0")
                .build();
    }

    @Bean
    public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder, OrderFactsTools orderFactsTools) {
        ChatClient chatClient = chatClientBuilder.clone()
                .defaultSystem(SYSTEM)
                .defaultTools(orderFactsTools)
                .build();

        return new DefaultAgentExecutor(chatClient, (chat, requestContext) -> {
            String userMessage = DefaultAgentExecutor.extractTextFromMessage(requestContext.getMessage());
            return chat.prompt().user(userMessage).call().content();
        });
    }
}
