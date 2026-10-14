package com.javagenai.kb.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** DeepSeek speaks the OpenAI chat protocol. Generator and judge differ only by model id. */
@ApplicationScoped
public class DeepSeekChatClient implements ChatClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http;
    private final URI completions;
    private final String apiKey;
    private final Duration timeout;

    @Inject
    public DeepSeekChatClient(
            @ConfigProperty(name = "kb.deepseek.base-url", defaultValue = "https://api.deepseek.com") String baseUrl,
            @ConfigProperty(name = "deepseek.api-key", defaultValue = "") String apiKey,
            @ConfigProperty(name = "kb.deepseek.timeout-seconds", defaultValue = "60") long timeoutSeconds) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), baseUrl, apiKey, timeoutSeconds);
    }

    DeepSeekChatClient(HttpClient http, String baseUrl, String apiKey, long timeoutSeconds) {
        String root = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = http;
        this.completions = URI.create(root + "/chat/completions");
        this.apiKey = apiKey == null ? "" : apiKey;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public ChatReply complete(String model, double temperature, String system, String user) {
        try {
            HttpRequest request = HttpRequest.newBuilder(completions)
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body(model, temperature, system, user)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("DeepSeek returned HTTP " + response.statusCode());
            }
            return parse(response.body());
        } catch (IOException e) {
            throw new IllegalStateException("DeepSeek call failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DeepSeek call was interrupted", e);
        }
    }

    private String body(String model, double temperature, String system, String user) throws IOException {
        ObjectNode root = JSON.createObjectNode();
        root.put("model", model);
        root.put("temperature", temperature);
        var messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", system);
        messages.addObject().put("role", "user").put("content", user);
        return JSON.writeValueAsString(root);
    }

    private ChatReply parse(String body) throws IOException {
        JsonNode root = JSON.readTree(body);
        String text = root.path("choices").path(0).path("message").path("content").asText("");
        JsonNode usage = root.path("usage");
        return new ChatReply(text, usage.path("prompt_tokens").asInt(0), usage.path("completion_tokens").asInt(0));
    }
}
