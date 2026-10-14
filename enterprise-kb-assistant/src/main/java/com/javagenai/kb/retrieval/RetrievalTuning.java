package com.javagenai.kb.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * One experiment's retrieval settings. Passed per call rather than read from
 * config at startup so the dashboard can tweak a single knob and rerun without
 * restarting the application — the whole point of the eval harness is comparing
 * these against each other.
 */
public record RetrievalTuning(Mode mode, double rrfK, int candidateK, boolean rerank) {

    public enum Mode {
        /** Vector + lexical fused with RRF. */
        HYBRID,
        /** Dense only — the naive baseline. */
        VECTOR_ONLY,
        /** Lexical only — measures what BM25 alone is worth. */
        LEXICAL_ONLY
    }

    public RetrievalTuning {
        if (rrfK <= 0) {
            throw new IllegalArgumentException("rrfK must be positive");
        }
        if (candidateK <= 0) {
            throw new IllegalArgumentException("candidateK must be positive");
        }
    }

    /** Stable, human-readable config for the run row and the dashboard's config column. */
    public String toJson() {
        ObjectNode json = new ObjectMapper().createObjectNode();
        json.put("mode", mode().name());
        json.put("rrfK", rrfK());
        json.put("candidateK", candidateK());
        json.put("rerank", rerank());
        return json.toString();
    }

    public String shortLabel() {
        return mode() + (rerank() ? "+rerank" : "") + ", rrf=" + rrfK() + ", top=" + candidateK();
    }
}