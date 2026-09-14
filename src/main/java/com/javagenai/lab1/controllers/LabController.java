package com.javagenai.lab1.controllers;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import com.javagenai.lab1.config.LabProperties;
import com.javagenai.lab1.services.ModelCall;
import com.javagenai.lab1.services.RefundLedger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LabController {

    private final ModelCall model;
    private final RefundLedger ledger;
    private final Executor llmFence;
    private final LabProperties lab;

    public LabController(
            ModelCall model,
            RefundLedger ledger,
            @Qualifier("llmFence") Executor llmFence,
            LabProperties lab) {
        this.model = model;
        this.ledger = ledger;
        this.llmFence = llmFence;
        this.lab = lab;
    }

    @GetMapping("/orders")
    Map<String, Object> orders() {
        return Map.of(
                "ok", true,
                "ms", 1,
                "thread", Thread.currentThread().getName());
    }

    @GetMapping("/chat/naive")
    Map<String, String> naive(String q) {
        String prompt = q == null || q.isBlank() ? "Reply with exactly: pong" : q;
        return Map.of(
                "mode", "naive",
                "thread", Thread.currentThread().getName(),
                "answer", model.complete(prompt));
    }

    @GetMapping("/chat/fenced")
    Map<String, String> fenced(String q) {
        String prompt = q == null || q.isBlank() ? "Reply with exactly: pong" : q;
        return java.util.concurrent.CompletableFuture.supplyAsync(
                        () -> Map.of(
                                "mode", "fenced",
                                "thread", Thread.currentThread().getName(),
                                "answer", model.complete(prompt)),
                        llmFence)
                .join();
    }

    @GetMapping("/refund/bad-retry")
    Map<String, Object> badRetry(String orderId) {
        String id = orderId == null ? "A-1" : orderId;
        RuntimeException last = new IllegalStateException("retries exhausted");
        for (int attempt = 1; attempt <= 3; attempt++) {
            ledger.refund(id);
            try {
                if (attempt < 3) {
                    throw new IllegalStateException("simulated model timeout on attempt " + attempt);
                }
                String answer = model.complete("One-line refund confirmation for order " + id);
                return Map.of("attempts", attempt, "refundsPosted", ledger.count(), "answer", answer);
            } catch (RuntimeException ex) {
                last = ex;
            }
        }
        throw last;
    }

    @GetMapping("/refund/idempotent")
    Map<String, Object> idempotent(String orderId) {
        String id = orderId == null ? "A-1" : orderId;
        int posted = ledger.refund(id);
        RuntimeException last = new IllegalStateException("retries exhausted");
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                if (attempt < 3) {
                    throw new IllegalStateException("simulated model timeout on attempt " + attempt);
                }
                String answer = model.complete("One-line refund confirmation for order " + id);
                return Map.of(
                        "attempts", attempt,
                        "refundsPosted", posted,
                        "note", "ledger incremented once, then the model was retried",
                        "answer",
                        answer);
            } catch (RuntimeException ex) {
                last = ex;
            }
        }
        throw last;
    }

    @PostMapping("/lab/reset-ledger")
    Map<String, Integer> reset() {
        ledger.reset();
        return Map.of("refundsPosted", 0);
    }

    @GetMapping("/lab/stats")
    Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tomcatHint", "server.tomcat.threads.max is 8 — naive chat holds these");
        out.put("liveThreads", ManagementFactory.getThreadMXBean().getThreadCount());
        out.put("extraHoldMs", lab.extraHoldMs());
        out.put("fenceSize", lab.fenceSize());
        out.put("refundsPosted", ledger.count());
        if (llmFence instanceof ThreadPoolTaskExecutor t) {
            ThreadPoolExecutor e = t.getThreadPoolExecutor();
            out.put("fenceActive", e.getActiveCount());
            out.put("fencePool", e.getPoolSize());
        }
        return out;
    }
}
