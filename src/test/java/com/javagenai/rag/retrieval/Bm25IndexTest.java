package com.javagenai.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.javagenai.rag.corpus.Passage;
import org.junit.jupiter.api.Test;

class Bm25IndexTest {

    @Test
    void rarePartNumberOutranksTheLookalikeGasket() {
        Bm25Index index = new Bm25Index(List.of(
                new Passage("warranty#1", "Pump warranty",
                        "The impeller housing uses gasket ZX-4419 during the first registered year."),
                new Passage("warranty#2", "Pump warranty",
                        "A lookalike gasket ZY-100 fits the older Coastal series and is never a warranty part.")));

        List<RankedPassage> hits = index.search("Who covers gasket ZX-4419?", 2);

        assertThat(hits).isNotEmpty();
        assertThat(hits.getFirst().passage().passageId()).isEqualTo("warranty#1");
    }
}
