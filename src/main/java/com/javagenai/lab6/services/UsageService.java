package com.javagenai.lab6.services;

import java.util.LinkedHashMap;
import java.util.Map;

import com.javagenai.lab6.observability.TokenUsageHandler;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class UsageService {

    private final MeterRegistry registry;

    public UsageService(MeterRegistry registry) {
        this.registry = registry;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> body = new LinkedHashMap<>();
        registry.find(TokenUsageHandler.TOKENS).counters().forEach(counter -> {
            String tenant = counter.getId().getTag("tenant");
            String model = counter.getId().getTag("model");
            String tokenType = counter.getId().getTag("token_type");
            body.put(tenant + "|" + model + "|" + tokenType, counter.count());
        });
        registry.find(TokenUsageHandler.CALLS).counters().forEach(counter ->
                body.put(counter.getId().getTag("tenant") + "|" + counter.getId().getTag("model") + "|calls", counter.count()));
        return body;
    }
}
