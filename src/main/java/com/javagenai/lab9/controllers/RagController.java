package com.javagenai.lab9.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.javagenai.lab9.services.KeywordRetriever;
import com.javagenai.lab9.services.PromptGate;
import com.javagenai.lab9.services.UnsafePromptRenderer;
import com.javagenai.lab9.vo.Prompt;
import com.javagenai.lab9.vo.RetrievedChunk;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RagController {

    private final KeywordRetriever retriever;
    private final PromptGate gate;
    private final UnsafePromptRenderer unsafe;

    public RagController(KeywordRetriever retriever, PromptGate gate, UnsafePromptRenderer unsafe) {
        this.retriever = retriever;
        this.gate = gate;
        this.unsafe = unsafe;
    }

    @GetMapping("/rag/naive")
    Map<String, Object> naive(String q) {
        List<RetrievedChunk> hits = retriever.retrieve(q);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mode", "naive");
        body.put("chunks", hits.stream().map(RetrievedChunk::id).toList());
        body.put("dropped", List.of());
        body.put("promptPreview", unsafe.render(q, hits));
        return body;
    }

    @GetMapping("/rag/guarded")
    Map<String, Object> guarded(String q) {
        List<RetrievedChunk> hits = retriever.retrieve(q);
        Prompt prompt = gate.assemble(q, hits);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mode", "guarded");
        body.put("chunks", prompt.admittedChunkIds());
        body.put("dropped", prompt.dropped());
        body.put("promptPreview", prompt.text());
        return body;
    }
}
