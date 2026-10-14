package com.javagenai.kb.eval;

/**
 * One graded question inside a pinned run. Context recall stays null until
 * a golden supporting set is supplied; the scatter simply omits that point.
 */
public record EvalCase(
        String caseId,
        String question,
        String userRole,
        String category,
        String answer,
        boolean abstained,
        boolean citationsValid,
        Double contextRecall,
        Double phraseHit,
        Double faithfulness,
        Double relevancy,
        Double completeness,
        long latencyMs,
        int promptTokens,
        int completionTokens,
        /**
         * Generation failed for this case (e.g. no API key). Recall may still
         * be recorded; Tier 2 columns are null. Counted separately so a run
         * can never quietly look good by having crashed.
         */
        boolean failed) {
}
