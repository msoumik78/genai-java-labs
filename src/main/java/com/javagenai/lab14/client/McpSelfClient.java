package com.javagenai.lab14.client;

import java.util.List;
import java.util.Map;

import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.stereotype.Component;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

@Component
public class McpSelfClient {

    private final ServletWebServerApplicationContext server;

    public McpSelfClient(ServletWebServerApplicationContext server) {
        this.server = server;
    }

    public List<String> listToolNames() {
        try (McpSyncClient client = open()) {
            return client.listTools().tools().stream().map(tool -> tool.name()).toList();
        }
    }

    public String call(String name, Map<String, Object> arguments) {
        try (McpSyncClient client = open()) {
            CallToolResult result = client.callTool(new CallToolRequest(name, arguments));
            if (Boolean.TRUE.equals(result.isError())) {
                throw new IllegalStateException(text(result));
            }
            return text(result);
        }
    }

    private McpSyncClient open() {
        String baseUrl = "http://127.0.0.1:" + server.getWebServer().getPort();
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(baseUrl)
                .endpoint("/mcp")
                .build();
        McpSyncClient client = McpClient.sync(transport)
                .clientInfo(new Implementation("lab14-self-check", "0.1.0"))
                .build();
        client.initialize();
        return client;
    }

    private static String text(CallToolResult result) {
        return result.content().stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .map(TextContent::text)
                .findFirst()
                .orElse("");
    }
}
