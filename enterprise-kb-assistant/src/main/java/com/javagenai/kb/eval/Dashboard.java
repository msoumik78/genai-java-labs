package com.javagenai.kb.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * One row per run, which is one step on the tuning roadmap. Tier 2 averages
 * ignore cases the judge skipped. The scatter only includes cases that have
 * both context recall and faithfulness, so a missing Tier 1 number cannot
 * pretend to agree with the judge.
 */
public final class Dashboard {

    private Dashboard() {
    }

    public static List<Row> rows(List<EvalRun> runs) {
        List<Row> rows = new ArrayList<>();
        for (EvalRun run : runs) {
            rows.add(new Row(
                    run.id().toString(),
                    run.label(),
                    shortConfig(run.configJson()),
                    run.cases().size(),
                    failedCount(run),
                    rate(run, EvalCase::citationsValid),
                    rate(run, EvalCase::abstained),
                    mean(run, EvalCase::contextRecall),
                    mean(run, EvalCase::phraseHit),
                    mean(run, EvalCase::faithfulness),
                    mean(run, EvalCase::relevancy),
                    mean(run, EvalCase::completeness),
                    meanLong(run)));
        }
        return List.copyOf(rows);
    }

    private static long failedCount(EvalRun run) {
        return run.cases().stream().filter(EvalCase::failed).count();
    }

    /**
     * Config JSON to a readable chip: "HYBRID+rerank rrf=60". Falls back to the
     * raw JSON if a future config shape does not parse, so the column never lies.
     */
    static String shortConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return "—";
        }
        try {
            var node = new ObjectMapper().readTree(configJson);
            String mode = node.path("mode").asText("HYBRID");
            boolean rerank = node.path("rerank").asBoolean(false);
            return mode + (rerank ? "+rerank" : "")
                    + " rrf=" + node.path("rrfK").asDouble(60)
                    + " top=" + node.path("candidateK").asInt(50);
        } catch (Exception e) {
            return configJson;
        }
    }

    /**
     * Per-metric change of a candidate run against the baseline: the number the
     * demo is built around. Nulls stay null — a metric that did not run must
     * not show a fabricated delta.
     */
    public static List<Delta> deltas(Row baseline, Row candidate) {
        List<Delta> deltas = new ArrayList<>();
        add(deltas, "Context recall (Tier 1)", baseline.contextRecall(), candidate.contextRecall());
        add(deltas, "Phrase hit (Tier 1)", baseline.phraseHit(), candidate.phraseHit());
        add(deltas, "Citation validity (Tier 1)", baseline.citationValidity(), candidate.citationValidity());
        add(deltas, "Faithfulness (Tier 2)", baseline.faithfulness(), candidate.faithfulness());
        add(deltas, "Relevancy (Tier 2)", baseline.relevancy(), candidate.relevancy());
        add(deltas, "Completeness (Tier 2)", baseline.completeness(), candidate.completeness());
        return List.copyOf(deltas);
    }

    private static void add(List<Delta> deltas, String metric, Double before, Double after) {
        if (before != null && after != null) {
            deltas.add(new Delta(metric, before, after, after - before));
        }
    }

    public static List<Point> recallAgainstFaithfulness(List<EvalRun> runs) {
        List<Point> points = new ArrayList<>();
        for (EvalRun run : runs) {
            for (EvalCase item : run.cases()) {
                if (item.contextRecall() != null && item.faithfulness() != null) {
                    points.add(new Point(run.label(), item.caseId(), item.contextRecall(), item.faithfulness()));
                }
            }
        }
        return List.copyOf(points);
    }

    private static Double rate(EvalRun run, java.util.function.Predicate<EvalCase> predicate) {
        if (run.cases().isEmpty()) {
            return null;
        }
        long hits = run.cases().stream().filter(predicate).count();
        return (double) hits / run.cases().size();
    }

    private static Double mean(EvalRun run, java.util.function.Function<EvalCase, Double> value) {
        double sum = 0;
        int count = 0;
        for (EvalCase item : run.cases()) {
            Double score = value.apply(item);
            if (score != null) {
                sum += score;
                count++;
            }
        }
        return count == 0 ? null : sum / count;
    }

    private static Double meanLong(EvalRun run) {
        if (run.cases().isEmpty()) {
            return null;
        }
        return run.cases().stream().mapToLong(EvalCase::latencyMs).average().orElse(0);
    }

    public record Row(
            String runId,
            String label,
            String config,
            int cases,
            long failed,
            Double citationValidity,
            Double abstainRate,
            Double contextRecall,
            Double phraseHit,
            Double faithfulness,
            Double relevancy,
            Double completeness,
            Double meanLatencyMs) {
    }

    /** One metric's movement: the before/after pair behind "71% to 89% and here's how". */
    public record Delta(String metric, Double before, Double after, double change) {
    }

    public record Point(String label, String caseId, double contextRecall, double faithfulness) {
    }
}
