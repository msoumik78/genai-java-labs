package com.javagenai.kb.eval;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** An immutable experiment: one config, one corpus, one list of cases. */
public record EvalRun(
        UUID id,
        String label,
        String gitSha,
        String configJson,
        String judgeModel,
        String corpusDigest,
        Instant createdAt,
        List<EvalCase> cases) {

    public EvalRun {
        cases = List.copyOf(cases == null ? List.of() : cases);
    }
}
