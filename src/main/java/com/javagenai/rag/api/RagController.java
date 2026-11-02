package com.javagenai.rag.api;

import com.javagenai.rag.answer.HybridRagService;
import com.javagenai.rag.answer.RagAnswer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class RagController {

    private final HybridRagService hybridRagService;

    public RagController(HybridRagService hybridRagService) {
        this.hybridRagService = hybridRagService;
    }

    @GetMapping("/rag")
    public RagAnswer ask(@RequestParam String question) {
        if (question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is required");
        }
        return hybridRagService.answer(question.strip());
    }
}
