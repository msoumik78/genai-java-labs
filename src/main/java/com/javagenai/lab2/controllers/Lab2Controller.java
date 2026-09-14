package com.javagenai.lab2.controllers;

import java.util.Map;

import com.javagenai.lab2.kb.CorpusSeeder;
import com.javagenai.lab2.kb.KnowledgeBase;
import com.javagenai.lab2.kb.RagService;
import com.javagenai.lab2.kb.RagService.Mode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Lab2Controller {

    private final RagService rag;
    private final KnowledgeBase kb;
    private final CorpusSeeder seeder;

    public Lab2Controller(RagService rag, KnowledgeBase kb, CorpusSeeder seeder) {
        this.rag = rag;
        this.kb = kb;
        this.seeder = seeder;
    }

    @GetMapping("/search/naive")
    Map<String, Object> naive(@RequestParam(required = false) String tenant, @RequestParam(required = false) String q) {
        return rag.search(Mode.naive, tenant, q);
    }

    @GetMapping("/search/sql")
    Map<String, Object> sql(@RequestParam(required = false) String tenant, @RequestParam(required = false) String q) {
        return rag.search(Mode.sql, tenant, q);
    }

    @GetMapping("/search/rls")
    Map<String, Object> rls(@RequestParam(required = false) String tenant, @RequestParam(required = false) String q) {
        return rag.search(Mode.rls, tenant, q);
    }

    @GetMapping("/search/prompt")
    Map<String, Object> prompt(@RequestParam(required = false) String tenant, @RequestParam(required = false) String q) {
        return rag.search(Mode.prompt, tenant, q);
    }

    @GetMapping("/ask/naive")
    Map<String, Object> askNaive(@RequestParam(required = false) String tenant, @RequestParam(required = false) String q) {
        return rag.ask(Mode.naive, tenant, q);
    }

    @GetMapping("/ask/sql")
    Map<String, Object> askSql(@RequestParam(required = false) String tenant, @RequestParam(required = false) String q) {
        return rag.ask(Mode.sql, tenant, q);
    }

    @GetMapping("/ask/prompt")
    Map<String, Object> askPrompt(@RequestParam(required = false) String tenant, @RequestParam(required = false) String q) {
        return rag.ask(Mode.prompt, tenant, q);
    }

    @GetMapping("/lab/docs")
    Object docs() {
        return kb.list();
    }

    @PostMapping("/lab/reseed")
    Map<String, Object> reseed() {
        return seeder.reseed();
    }
}
