package com.javagenai.lab13.eval;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.metrics.nlp.BleuScoreMetric;
import ai.qa.solutions.sample.Sample;
import org.junit.jupiter.api.Test;

class BleuScoreSmokeTest {

    @Test
    void exactMatchScoresOne() {
        BleuScoreMetric metric = new BleuScoreMetric();
        Sample sample = Sample.builder()
                .response("A refund arrives in 5 business days.")
                .reference("A refund arrives in 5 business days.")
                .build();

        Double score = metric.singleTurnScore(
                BleuScoreMetric.BleuScoreConfig.builder().maxNgram(4).smoothing(true).build(),
                sample);

        assertThat(score).isGreaterThanOrEqualTo(0.99);
    }
}
