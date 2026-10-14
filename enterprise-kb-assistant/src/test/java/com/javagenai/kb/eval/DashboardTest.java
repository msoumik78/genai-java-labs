package com.javagenai.kb.eval;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DashboardTest {

    @Test
    void averagesTierTwoAndLeavesAbstentionsOutOfFaithfulness() {
        EvalRun run = run("3. rerank", List.of(
                graded("a", false, true, 0.4, 1.0, 1.0, 1.0, 40),
                graded("b", true, true, null, null, null, null, 5)));

        Dashboard.Row row = Dashboard.rows(List.of(run)).getFirst();

        assertThat(row.label()).isEqualTo("3. rerank");
        assertThat(row.citationValidity()).isEqualTo(1.0);
        assertThat(row.abstainRate()).isEqualTo(0.5);
        assertThat(row.faithfulness()).isEqualTo(1.0);
        assertThat(row.meanLatencyMs()).isEqualTo(22.5);
    }

    @Test
    void scatterDropsCasesMissingEitherAxis() {
        EvalRun baseline = run("1. baseline", List.of(
                graded("a", false, true, 0.2, 0.25, 1.0, 1.0, 10),
                graded("b", false, false, null, 0.9, 1.0, 1.0, 10)));
        EvalRun hybrid = run("2. hybrid", List.of(
                graded("a", false, true, 0.8, 0.7, 1.0, 1.0, 12)));

        List<Dashboard.Point> points = Dashboard.recallAgainstFaithfulness(List.of(baseline, hybrid));

        assertThat(points).extracting(Dashboard.Point::label).containsExactly("1. baseline", "2. hybrid");
        assertThat(points.getFirst().contextRecall()).isEqualTo(0.2);
        assertThat(points.get(1).faithfulness()).isEqualTo(0.7);
    }

    private EvalRun run(String label, List<EvalCase> cases) {
        return new EvalRun(UUID.randomUUID(), label, "abc", "{}", "deepseek-flash", "digest", Instant.EPOCH, cases);
    }

    private EvalCase graded(
            String id, boolean abstained, boolean citationsValid, Double recall,
            Double faithfulness, Double relevancy, Double completeness, long latencyMs) {
        return new EvalCase(id, "q", "EMPLOYEE", "answerable", "answer",
                abstained, citationsValid, recall, null, faithfulness, relevancy, completeness,
                latencyMs, 10, 4, false);
    }
}
