package com.javagenai.kb.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.javagenai.kb.retrieval.RetrievalTuning;
import org.junit.jupiter.api.Test;

class RetrievalTuningTest {

    @Test
    void serialisesEveryKnobSoRunsAreReproducible() {
        RetrievalTuning tuning =
                new RetrievalTuning(RetrievalTuning.Mode.VECTOR_ONLY, 12, 25, false);

        String json = tuning.toJson();

        assertThat(json).contains("\"mode\":\"VECTOR_ONLY\"")
                .contains("\"rrfK\":12.0")
                .contains("\"candidateK\":25")
                .contains("\"rerank\":false");
    }

    @Test
    void dashboardRendersConfigAsAReadableChip() {
        String json = new RetrievalTuning(RetrievalTuning.Mode.HYBRID, 60, 50, true).toJson();

        assertThat(Dashboard.shortConfig(json)).isEqualTo("HYBRID+rerank rrf=60.0 top=50");
    }

    @Test
    void unknownConfigShapeFallsBackToRawJson() {
        assertThat(Dashboard.shortConfig("{\"weird\":1}")).isNotBlank();
        assertThat(Dashboard.shortConfig("")).isEqualTo("—");
        assertThat(Dashboard.shortConfig(null)).isEqualTo("—");
    }

    @Test
    void rejectsNonsenseParameters() {
        assertThatThrownBy(() -> new RetrievalTuning(RetrievalTuning.Mode.HYBRID, 0, 50, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetrievalTuning(RetrievalTuning.Mode.HYBRID, 60, 0, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}