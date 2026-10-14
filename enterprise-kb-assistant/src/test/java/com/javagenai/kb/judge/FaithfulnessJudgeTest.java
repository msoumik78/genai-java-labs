package com.javagenai.kb.judge;

import static org.assertj.core.api.Assertions.assertThat;

import com.javagenai.kb.domain.Chunk;
import com.javagenai.kb.generation.ChatReply;
import com.javagenai.kb.retrieval.RetrievalResult;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FaithfulnessJudgeTest {

    @Test
    void faithfulnessIsTheFractionOfSupportedClaims() {
        AtomicReference<String> model = new AtomicReference<>();
        FaithfulnessJudge judge = new FaithfulnessJudge((name, temperature, system, user) -> {
            model.set(name);
            return new ChatReply("""
                    ```json
                    {"claims":[
                      {"text":"12 weeks","verdict":"SUPPORTED"},
                      {"text":"unlimited","verdict":"CONTRADICTED"}
                    ],"relevancy":0.9,"completeness":1.0}
                    ```
                    """, 20, 30);
        }, "deepseek-flash", 0.0);

        Judgement judgement = judge.judge(
                "How much leave?",
                "12 weeks [1], and it is unlimited [1].",
                false,
                List.of(hit()));

        assertThat(model).hasValue("deepseek-flash");
        assertThat(judgement.faithfulness()).isEqualTo(0.5);
        assertThat(judgement.relevancy()).isEqualTo(0.9);
        assertThat(judgement.completeness()).isEqualTo(1.0);
        assertThat(judgement.claims()).hasSize(2);
    }

    @Test
    void skipsTheModelWhenTheAnswerAbstained() {
        AtomicReference<String> called = new AtomicReference<>();
        FaithfulnessJudge judge = new FaithfulnessJudge((name, temperature, system, user) -> {
            called.set(name);
            return new ChatReply("{}", 0, 0);
        }, "deepseek-flash", 0.0);

        Judgement judgement = judge.judge("Salary?", "ABSTAIN", true, List.of(hit()));

        assertThat(called.get()).isNull();
        assertThat(judgement.faithfulness()).isNull();
        assertThat(judgement.judgeModel()).isEqualTo("deepseek-flash");
    }

    private RetrievalResult hit() {
        return new RetrievalResult(
                new Chunk("chunk-leave", "doc", "s", "12 weeks of leave", "Leave", List.of()), 1, 1, 0);
    }
}
