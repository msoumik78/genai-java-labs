package com.javagenai.lab7.controllers;

import java.util.Map;

import com.javagenai.lab7.services.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat")
    Map<String, Object> chat(@RequestParam(defaultValue = "What is a semantic cache?") String q) {
        return chatService.chat(q);
    }
}
