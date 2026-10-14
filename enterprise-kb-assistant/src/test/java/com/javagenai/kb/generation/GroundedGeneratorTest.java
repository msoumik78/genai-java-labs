package com.javagenai.kb.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.javagenai.kb.domain.Chunk;
import com.javagenai.kb.retrieval.RetrievalResult;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GroundedGeneratorTest {

    @Test
    void mapsInRangeMarkersToChunkIds() {
        GroundedGenerator generator = generator((model, temperature, system, user) ->
                new ChatReply("Employees receive 12 weeks [1].", 10, 4));

        GeneratedAnswer answer = generator.answer("How much leave?", List.of(hit("chunk-leave", "12 weeks of leave")));

        assertThat(answer.abstained()).isFalse();
        assertThat(answer.citationsValid()).isTrue();
        assertThat(answer.citedChunkIds()).containsExactly("chunk-leave");
        assertThat(modelSeen).isEqualTo("deepseek-v4-pro");
    }

    @Test
    void rejectsAMarkerPastTheContext() {
        GroundedGenerator generator = generator((model, temperature, system, user) ->
                new ChatReply("See the handbook [2].", 8, 3));

        GeneratedAnswer answer = generator.answer("Where?", List.of(hit("chunk-leave", "12 weeks")));

        assertThat(answer.citationsValid()).isFalse();
        assertThat(answer.citedChunkIds()).isEmpty();
    }

    @Test
    void abstainsWhenTheModelRefuses() {
        GroundedGenerator generator = generator((model, temperature, system, user) ->
                new ChatReply("ABSTAIN", 5, 1));

        GeneratedAnswer answer = generator.answer("What is the CEO's salary?", List.of(hit("chunk-leave", "12 weeks")));

        assertThat(answer.abstained()).isTrue();
        assertThat(answer.citationsValid()).isTrue();
    }

    @Test
    void abstainsWithoutCallingTheModelWhenNothingWasRetrieved() {
        AtomicInteger calls = new AtomicInteger();
        GroundedGenerator generator = generator((model, temperature, system, user) -> {
            calls.incrementAndGet();
            return new ChatReply("should not run", 0, 0);
        });

        GeneratedAnswer answer = generator.answer("Anything?", List.of());

        assertThat(answer.abstained()).isTrue();
        assertThat(calls).hasValue(0);
    }

    private String modelSeen;

    private GroundedGenerator generator(ChatClient client) {
        return new GroundedGenerator(new ChatClient() {
            @Override
            public ChatReply complete(String model, double temperature, String system, String user) {
                modelSeen = model;
                return client.complete(model, temperature, system, user);
            }
        }, "deepseek-v4-pro", 0.0);
    }

    private RetrievalResult hit(String id, String text) {
        return new RetrievalResult(new Chunk(id, "doc", "s", text, "Leave", List.of()), 1.0, 1, 0);
    }
}
