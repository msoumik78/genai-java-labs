package com.javagenai.a2a.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HostClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(HostClientApplication.class, args);
    }

    @Bean
    public ChatClient routingChatClient(ChatClient.Builder chatClientBuilder, RemoteOrderAgent remoteOrderAgent) {
        String system = """
                You answer order questions by delegating to a remote A2A agent.
                Call sendToOrderAgent with the user's question.
                Return the remote agent's answer unchanged.
                Discovered agent:
                %s
                """.formatted(remoteOrderAgent.describe());
        return chatClientBuilder.defaultSystem(system).defaultTools(remoteOrderAgent).build();
    }
}
