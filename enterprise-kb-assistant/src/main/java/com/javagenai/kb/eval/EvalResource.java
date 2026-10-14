package com.javagenai.kb.eval;

import com.javagenai.kb.retrieval.RetrievalTuning;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Runs one experiment and pins it. Batch and synchronous on purpose: a run
 * should be a deliberate, reviewable act (it becomes a dashboard row), not a
 * side effect of browsing.
 */
@Path("/eval")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EvalResource {

    @Inject
    EvalRunner runner;

    @POST
    @Path("/run")
    public EvalSummary run(RunRequest request) {
        List<GoldenCase> dataset = GoldenDataset.load(datasetPath(request));
        EvalRun run = runner.run(request.label(), request.gitSha(), dataset, tuning(request));
        long failed = run.cases().stream().filter(EvalCase::failed).count();
        return new EvalSummary(run.id().toString(), run.label(), run.cases().size(), failed);
    }

    /** Form/JSON knobs default to the configured values when a knob is omitted. */
    private RetrievalTuning tuning(RunRequest request) {
        RetrievalTuning.Mode mode = request.mode() == null
                ? RetrievalTuning.Mode.HYBRID
                : RetrievalTuning.Mode.valueOf(request.mode().toUpperCase());
        double rrfK = request.rrfK() == null ? 60 : request.rrfK();
        int candidateK = request.candidateK() == null ? 50 : request.candidateK();
        boolean rerank = request.rerank() == null || request.rerank();
        return new RetrievalTuning(mode, rrfK, candidateK, rerank);
    }

    private java.nio.file.Path datasetPath(RunRequest request) {
        if (request.datasetPath() != null && !request.datasetPath().isBlank()) {
            return java.nio.file.Path.of(request.datasetPath());
        }
        return java.nio.file.Path.of("eval", "dataset", "golden.jsonl");
    }

    /**
     * All knobs are optional; JSON omits what the form did not send, so the
     * dashboard defaults match application.yaml.
     */
    public record RunRequest(
            String label,
            String gitSha,
            String datasetPath,
            String mode,
            Double rrfK,
            Integer candidateK,
            Boolean rerank) {
    }

    public record EvalSummary(String runId, String label, int cases, long failed) {
    }
}