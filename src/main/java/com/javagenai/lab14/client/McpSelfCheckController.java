package com.javagenai.lab14.client;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lab")
public class McpSelfCheckController {

    private final McpSelfClient client;

    public McpSelfCheckController(McpSelfClient client) {
        this.client = client;
    }

    @GetMapping("/tools")
    public List<String> tools() {
        return client.listToolNames();
    }

    @PostMapping("/call")
    public String call(@RequestBody CallRequest request) {
        Map<String, Object> arguments = request.arguments() == null ? Map.of() : request.arguments();
        return client.call(request.name(), arguments);
    }

    public record CallRequest(String name, Map<String, Object> arguments) {
    }
}
