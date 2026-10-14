package com.javagenai.kb.eval;

import com.javagenai.kb.generation.GeneratedAnswer;
import com.javagenai.kb.generation.GroundedGenerator;
import com.javagenai.kb.judge.FaithfulnessJudge;
import com.javagenai.kb.judge.Judgement;
import com.javagenai.kb.retrieval.HybridRetriever;
import com.javagenai.kb.retrieval.RetrievalResult;
import com.javagenai.kb.retrieval.RetrievalTuning;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Runs the golden dataset through the real pipeline and grades every case.
 *
 * <p>Tier 1 (deterministic, no model): document-level context recall, phrase
 * hits, abstention match, citation validity. Tier 2 (LLM judge): faithfulness,
 * relevancy, completeness. A generation failure (missing API key, network)
 * marks the case failed but still records its Tier 1 recall — retrieval
 * experiments must remain runnable without credentials.
 */
@ApplicationScoped
public class EvalRunner {

    private static final Logger LOG = Logger.getLogger(EvalRunner.class);

    private final HybridRetriever retriever;
    private final GroundedGenerator generator;
    private final FaithfulnessJudge judge;
    private final EvaluationStore store;

    @Inject
    public EvalRunner(
            HybridRetriever retriever,
            GroundedGenerator generator,
            FaithfulnessJudge judge,
            EvaluationStore store) {
        this.retriever = retriever;
        this.generator = generator;
        this.judge = judge;
        this.store = store;
    }

    public EvalRun run(String label, String gitSha, List<GoldenCase> dataset, RetrievalTuning tuning) {
        List<EvalCase> graded = new ArrayList<>();
        for (GoldenCase golden : dataset) {
            graded.add(evaluate(golden, tuning));
        }
        EvalRun run = new EvalRun(
                UUID.randomUUID(), label, gitSha, tuning.toJson(), judgeModel(), corpusDigest(), null, graded);
        store.save(run);
        return run;
    }

    private EvalCase evaluate(GoldenCase golden, RetrievalTuning tuning) {
        List<RetrievalResult> context = retriever.retrieve(golden.question(), golden.role(), 5, tuning);
        Double recall = recall(golden, context);
        GeneratedAnswer answer;
        try {
            answer = generator.answer(golden.question(), context);
        } catch (RuntimeException e) {
            LOG.warnf("Generation failed for %s, recording Tier 1 only: %s", golden.id(), e.getMessage());
            return failedCase(golden, recall);
        }
        return judgedCase(golden, context, answer, recall);
    }

    /** Keeps recall (retrieval is intact) but marks the case so the dashboard can count it. */
    private EvalCase failedCase(GoldenCase golden, Double recall) {
        return new EvalCase(
                golden.id(), golden.question(), golden.role(), golden.category(),
                "", false, false, recall, null, null, null, null, 0, 0, 0, true);
    }

    private EvalCase judgedCase(GoldenCase golden, List<RetrievalResult> context,
            GeneratedAnswer answer, Double recall) {
        Judgement verdict;
        try {
            verdict = judge.judge(golden.question(), answer.text(), answer.abstained(), context);
        } catch (RuntimeException e) {
            LOG.warnf("Judge failed for %s, Tier 2 null: %s", golden.id(), e.getMessage());
            verdict = new Judgement(null, null, null, List.of(), judgeModel());
        }
        return new EvalCase(
                golden.id(),
                golden.question(),
                golden.role(),
                golden.category(),
                answer.text(),
                answer.abstained(),
                answer.citationsValid(),
                recall,
                phraseHit(golden, answer),
                verdict.faithfulness(),
                verdict.relevancy(),
                verdict.completeness(),
                answer.latencyMs(),
                answer.promptTokens(),
                answer.completionTokens(),
                false);
    }

    /**
     * Document-level recall: the fraction of golden supporting documents that
     * appear among the retrieved chunks. Null when the case has no supporting
     * set (unanswerable / ACL cases), so Tier 2 never pretends to agree.
     */
    private Double recall(GoldenCase golden, List<RetrievalResult> context) {
        if (golden.supportingDocs().isEmpty()) {
            return null;
        }
        Set<String> retrievedDocs = context.stream()
                .map(result -> result.chunk().documentId())
                .collect(Collectors.toSet());
        long hits = golden.supportingDocs().stream().filter(retrievedDocs::contains).count();
        return (double) hits / golden.supportingDocs().size();
    }

    /**
     * Tier 1 answer quality: the fraction of golden phrases present in the
     * answer (case-insensitive). Null when the case has no phrase expectations.
     */
    private Double phraseHit(GoldenCase golden, GeneratedAnswer answer) {
        if (golden.mustContain().isEmpty() || answer.abstained()) {
            return null;
        }
        String text = answer.text().toLowerCase();
        long hits = golden.mustContain().stream()
                .filter(phrase -> text.contains(phrase.toLowerCase()))
                .count();
        return (double) hits / golden.mustContain().size();
    }

    private String judgeModel() {
        return "deepseek-flash";
    }

    private String corpusDigest() {
        return "committed-corpus-v1";
    }
}