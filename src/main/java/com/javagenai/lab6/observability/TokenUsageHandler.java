package com.javagenai.lab6.observability;

import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationHandler;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.stereotype.Component;

/**
 * Spring AI already emits a chat span. Token counts ride on that span as
 * high-cardinality keys, which Prometheus will not aggregate. This copies the
 * exact usage onto counters tagged by tenant and model.
 */
@Component
public class TokenUsageHandler implements ObservationHandler<ChatModelObservationContext>, ObservationFilter {

    public static final String TOKENS = "genai.chat.tokens";
    public static final String CALLS = "genai.chat.calls";

    private final MeterRegistry registry;

    public TokenUsageHandler(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Observation.Context map(Observation.Context context) {
        if (context instanceof ChatModelObservationContext) {
            context.addLowCardinalityKeyValue(KeyValue.of("tenant", TenantHolder.current()));
        }
        return context;
    }

    @Override
    public void onStop(ChatModelObservationContext context) {
        ChatResponse response = context.getResponse();
        if (response == null || response.getMetadata() == null || response.getMetadata().getUsage() == null) {
            return;
        }
        Usage usage = response.getMetadata().getUsage();
        String tenant = TenantHolder.current();
        String model = response.getMetadata().getModel() == null ? "unknown" : response.getMetadata().getModel();

        add(tenant, model, "input", usage.getPromptTokens());
        add(tenant, model, "output", usage.getCompletionTokens());
        registry.counter(CALLS, "tenant", tenant, "model", model).increment();
    }

    private void add(String tenant, String model, String tokenType, Integer tokens) {
        if (tokens == null || tokens < 0) {
            return;
        }
        registry.counter(TOKENS, "tenant", tenant, "model", model, "token_type", tokenType).increment(tokens);
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext;
    }
}
