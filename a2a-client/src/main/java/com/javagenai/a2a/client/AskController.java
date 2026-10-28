package com.javagenai.a2a.client;

import java.util.LinkedHashMap;
import java.util.Map;

import io.a2a.spec.AgentCard;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AskController {

    private final ChatClient routingChatClient;
    private final RemoteOrderAgent remoteOrderAgent;

    public AskController(ChatClient routingChatClient, RemoteOrderAgent remoteOrderAgent) {
        this.routingChatClient = routingChatClient;
        this.remoteOrderAgent = remoteOrderAgent;
    }

    @GetMapping("/discover")
    public Map<String, Object> discover() {
        AgentCard card = remoteOrderAgent.discover();
        Map<String, Object> body = new LinkedHashMap<>();
        if (card == null) {
            body.put("discovered", false);
            return body;
        }
        body.put("discovered", true);
        body.put("name", card.name());
        body.put("description", card.description());
        body.put("url", card.url());
        body.put("skills", card.skills());
        return body;
    }

    @GetMapping("/ask")
    public Map<String, String> ask(@RequestParam(name = "q") String question) {
        String answer = routingChatClient.prompt().user(question).call().content();
        Map<String, String> body = new LinkedHashMap<>();
        body.put("question", question);
        body.put("agent", remoteOrderAgent.describe());
        body.put("answer", answer);
        return body;
    }
}
