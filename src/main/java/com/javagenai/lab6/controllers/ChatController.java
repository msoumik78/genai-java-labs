package com.javagenai.lab6.controllers;

import java.util.Map;

import com.javagenai.lab6.services.ChatService;
import com.javagenai.lab6.services.UsageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatService chatService;
    private final UsageService usageService;

    public ChatController(ChatService chatService, UsageService usageService) {
        this.chatService = chatService;
        this.usageService = usageService;
    }

    @GetMapping("/chat")
    Map<String, Object> chat(
            @RequestParam(defaultValue = "Say hello in one sentence.") String q,
            @RequestParam(defaultValue = "anonymous") String tenant) {
        return chatService.chat(q, tenant);
    }

    @GetMapping("/usage")
    Map<String, Object> usage() {
        return usageService.snapshot();
    }
}
