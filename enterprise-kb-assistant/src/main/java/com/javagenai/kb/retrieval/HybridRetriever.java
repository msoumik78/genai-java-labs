package com.javagenai.kb.retrieval;

import com.javagenai.kb.domain.Chunk;
import com.javagenai.kb.embedding.NomicEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Retrieval in three stages: both arms rank independently (each already
 * ACL-filtered in SQL), RRF fuses them by position, and a cross-encoder
 * reranks the fused pool. The per-call {@link RetrievalTuning} is what the
 * dashboard knobs change — mode picks the arms, the rest are parameters.
 */
@ApplicationScoped
public class HybridRetriever {

    @Inject
    NomicEmbeddingModel embeddingModel;

    @Inject
    ChunkStore store;

    @Inject
    Reranker reranker;

    @ConfigProperty(name = "kb.retrieval.candidate-k", defaultValue = "50")
    int candidateK;

    @ConfigProperty(name = "kb.retrieval.rrf-k", defaultValue = "60")
    double rrfK;

    @ConfigProperty(name = "kb.reranker.enabled", defaultValue = "true")
    boolean rerankEnabled;

    /** The configured default: what /demo/ask uses when no experiment overrides it. */
    public RetrievalTuning defaultTuning() {
        return new RetrievalTuning(RetrievalTuning.Mode.HYBRID, rrfK, candidateK, rerankEnabled);
    }

    public List<RetrievalResult> retrieve(String question, String role, int limit) {
        return retrieve(question, role, limit, defaultTuning());
    }

    /** Fuses (or a single arm, per mode), optionally reranks, then cuts to limit. */
    public List<RetrievalResult> retrieve(String question, String role, int limit, RetrievalTuning tuning) {
        List<Chunk> byMeaning = usesVector(tuning)
                ? store.searchByVector(embed(question), role, tuning.candidateK())
                : List.of();
        List<Chunk> byWords = usesLexical(tuning)
                ? store.searchLexically(question, role, tuning.candidateK())
                : List.of();

        List<RetrievalResult> fused = new ReciprocalRankFusion(tuning.rrfK()).fuse(byMeaning, byWords);
        List<RetrievalResult> ranked = tuning.rerank()
                ? reranker.rerank(question, fused)
                : fused;
        return ranked.stream().limit(limit).toList();
    }

    private boolean usesVector(RetrievalTuning tuning) {
        return tuning.mode() != RetrievalTuning.Mode.LEXICAL_ONLY;
    }

    private boolean usesLexical(RetrievalTuning tuning) {
        return tuning.mode() != RetrievalTuning.Mode.VECTOR_ONLY;
    }

    private float[] embed(String question) {
        Embedding query = embeddingModel.embedQuery(question);
        return query.vector();
    }
}