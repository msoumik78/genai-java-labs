package com.javagenai.lab1.services;

import com.javagenai.lab1.config.LabProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ModelCall {

    private final ChatClient chat;
    private final LabProperties lab;

    public ModelCall(ChatClient.Builder builder, LabProperties lab) {
        this.chat = builder.build();
        this.lab = lab;
    }

    public String complete(String userText) {
        hold();
        return chat.prompt().user(userText).call().content();
    }

    private void hold() {
        if (lab.extraHoldMs() <= 0) {
            return;
        }
        try {
            Thread.sleep(lab.extraHoldMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while simulating a slow model", e);
        }
    }
}
