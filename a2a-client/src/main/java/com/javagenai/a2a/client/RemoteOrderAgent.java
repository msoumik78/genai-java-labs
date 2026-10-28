package com.javagenai.a2a.client;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RemoteOrderAgent {

    private static final Logger log = LoggerFactory.getLogger(RemoteOrderAgent.class);

    private final String agentUrl;
    private volatile AgentCard card;

    public RemoteOrderAgent(@Value("${remote.agent.url}") String agentUrl) {
        this.agentUrl = agentUrl;
        discover();
    }

    public AgentCard discover() {
        try {
            String path = new URI(agentUrl).getPath();
            String cardPath = path + ".well-known/agent-card.json";
            log.info("Fetching Agent Card from {}{}", agentUrl, cardPath);
            AgentCard resolved = A2A.getAgentCard(agentUrl, cardPath, null);
            this.card = resolved;
            log.info("Discovered agent '{}' skills={}", resolved.name(), resolved.skills());
            return resolved;
        }
        catch (Exception ex) {
            log.warn("Agent Card not available yet at {}: {}", agentUrl, ex.getMessage());
            return this.card;
        }
    }

    public String describe() {
        AgentCard current = this.card;
        if (current == null) {
            return "(not discovered yet — start the facts server, then GET /discover)";
        }
        return current.name() + ": " + current.description() + " skills=" + current.skills();
    }

    public AgentCard card() {
        return this.card;
    }

    @Tool(description = "Send the user's order question to the remote Order Facts agent discovered via A2A.")
    public String sendToOrderAgent(@ToolParam(description = "The full user question to forward") String question) {
        AgentCard current = this.card != null ? this.card : discover();
        if (current == null) {
            return "Remote agent card has not been discovered. Start the facts server and retry.";
        }

        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart(question, null)))
                .build();

        CompletableFuture<String> response = new CompletableFuture<>();
        BiConsumer<ClientEvent, AgentCard> consumer = (event, ignored) -> {
            if (event instanceof TaskEvent taskEvent) {
                response.complete(textFrom(taskEvent.getTask()));
            }
        };

        try {
            Client client = Client.builder(current)
                    .clientConfig(new ClientConfig.Builder().setAcceptedOutputModes(List.of("text")).build())
                    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                    .addConsumers(List.of(consumer))
                    .build();
            log.info("A2A message/send to '{}' : {}", current.name(), question);
            client.sendMessage(message);
            return response.get(120, TimeUnit.SECONDS);
        }
        catch (Exception ex) {
            log.error("A2A call failed: {}", ex.getMessage());
            return "A2A call failed: " + ex.getMessage();
        }
    }

    private static String textFrom(Task task) {
        if (task.getArtifacts() == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (Artifact artifact : task.getArtifacts()) {
            if (artifact.parts() == null) {
                continue;
            }
            for (Part<?> part : artifact.parts()) {
                if (part instanceof TextPart textPart) {
                    text.append(textPart.getText());
                }
            }
        }
        return text.toString();
    }
}
