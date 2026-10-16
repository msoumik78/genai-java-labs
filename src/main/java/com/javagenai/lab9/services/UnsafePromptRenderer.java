package com.javagenai.lab9.services;

import java.util.List;

import com.javagenai.lab9.vo.RetrievedChunk;
import org.springframework.stereotype.Component;

/**
 * Bypass used only by the naive endpoint. A real service would not have this type.
 * Retrieved text is concatenated straight into the prompt.
 */
@Component
public class UnsafePromptRenderer {

    public String render(String question, List<RetrievedChunk> retrieved) {
        StringBuilder body = new StringBuilder();
        body.append("Use the documents below to answer.\n\n");
        for (RetrievedChunk chunk : retrieved) {
            body.append(chunk.id()).append(": ").append(chunk.text()).append("\n\n");
        }
        body.append("Question: ").append(question == null ? "" : question.strip());
        return body.toString();
    }
}
