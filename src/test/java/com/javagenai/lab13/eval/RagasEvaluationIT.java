package com.javagenai.lab13.eval;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.metrics.nlp.BleuScoreMetric;
import ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
import ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;
import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import ai.qa.solutions.sample.Sample;
import com.javagenai.lab13.rag.AskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RagasEvaluationIT {

    private static final double GROUNDED_FAITHFULNESS = 0.5;
    private static final double GROUNDED_RELEVANCY = 0.7;
    private static final double GROUNDED_RECALL = 0.9;
    private static final double GROUNDED_BLEU = 0.15;
    private static final double MISSING_RELEVANCY = 0.4;

    @Autowired
    private AskService askService;

    @Autowired
    private FaithfulnessMetric faithfulnessMetric;

    @Autowired
    private ResponseRelevancyMetric responseRelevancyMetric;

    @Autowired
    private ContextRecallMetric contextRecallMetric;

    private final BleuScoreMetric bleuScoreMetric = new BleuScoreMetric();

    @Test
    void refundAnswerMeetsQualityBar() {
        Scores scores = score(
                "How many business days until a refund arrives?",
                "A refund arrives in 5 business days.");

        assertThat(scores.faithfulness).isGreaterThanOrEqualTo(GROUNDED_FAITHFULNESS);
        assertThat(scores.responseRelevancy).isGreaterThanOrEqualTo(GROUNDED_RELEVANCY);
        assertThat(scores.contextRecall).isGreaterThanOrEqualTo(GROUNDED_RECALL);
        assertThat(scores.bleu).isGreaterThanOrEqualTo(GROUNDED_BLEU);
    }

    @Test
    void missingFactIsNotTreatedAsAnswered() {
        Scores scores = score(
                "What is the guest wifi password?",
                "The guest wifi password is lantern-42.");

        assertThat(scores.contextRecall).isZero();
        assertThat(scores.responseRelevancy).isLessThan(MISSING_RELEVANCY);
    }

    private Scores score(String question, String reference) {
        AskService.Answer answer = askService.answer(question);
        Sample sample = Sample.builder()
                .userInput(answer.question())
                .response(answer.response())
                .retrievedContexts(answer.contexts())
                .reference(reference)
                .build();
        return new Scores(
                faithfulnessMetric.singleTurnScore(sample),
                responseRelevancyMetric.singleTurnScore(
                        ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig(), sample),
                contextRecallMetric.singleTurnScore(
                        ContextRecallMetric.ContextRecallConfig.builder().build(), sample),
                bleuScoreMetric.singleTurnScore(
                        BleuScoreMetric.BleuScoreConfig.builder().maxNgram(4).smoothing(true).build(), sample));
    }

    private record Scores(double faithfulness, double responseRelevancy, double contextRecall, double bleu) {
    }
}
