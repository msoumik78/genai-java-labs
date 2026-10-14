package com.javagenai.kb.judge;

import java.util.List;

/**
 * Tier 2 scores for one answer. Faithfulness is the fraction of claims the
 * judge marked SUPPORTED. Null scores mean the judge did not run, which is
 * what an abstention looks like.
 */
public record Judgement(
        Double faithfulness,
        Double relevancy,
        Double completeness,
        List<Claim> claims,
        String judgeModel) {

    public Judgement {
        claims = List.copyOf(claims == null ? List.of() : claims);
    }

    public record Claim(String text, String verdict) {
    }
}
