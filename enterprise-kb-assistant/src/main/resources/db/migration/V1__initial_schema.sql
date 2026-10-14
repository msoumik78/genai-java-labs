-- One Postgres holds vectors, the lexical index and access control, so that
-- role filtering happens inside the index scan rather than after ranking.
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE documents (
    id              TEXT PRIMARY KEY,
    title           TEXT        NOT NULL,
    filename        TEXT        NOT NULL,
    allowed_roles   TEXT[]      NOT NULL DEFAULT '{}',
    page_count      INT         NOT NULL DEFAULT 0,
    character_count INT         NOT NULL DEFAULT 0,
    table_count     INT         NOT NULL DEFAULT 0,
    -- Pinned so a re-ingest with a different parser cannot silently invalidate
    -- every experiment that references this document.
    parser          TEXT        NOT NULL,
    content_digest  TEXT,
    ingested_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The embedding dimension is fixed here rather than left free, because a change
-- to it is a schema change and should be a deliberate one.
CREATE TABLE chunks (
    id           TEXT PRIMARY KEY,
    document_id  TEXT        NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    section_id   TEXT        NOT NULL,
    section_path TEXT        NOT NULL DEFAULT '',
    body         TEXT        NOT NULL,
    roles        TEXT[]      NOT NULL DEFAULT '{}',
    embedding    vector(512),
    -- RRF consumes ranks, not scores: cosine and BM25 are not comparable
    -- without calibration, so the two result sets are fused by position.
    search_vector tsvector GENERATED ALWAYS AS (to_tsvector('english', body)) STORED
);

-- The GIN index is what makes the BM25 arm of hybrid search cheap.
CREATE INDEX chunks_search_vector_idx ON chunks USING GIN (search_vector);
CREATE INDEX chunks_embedding_hnsw_idx ON chunks USING hnsw (embedding vector_cosine_ops);
CREATE INDEX chunks_roles_idx ON chunks USING GIN (roles);
CREATE INDEX chunks_document_idx ON chunks (document_id);

-- Every experiment is an immutable row. The dashboard compares a candidate run
-- against a pinned baseline, so config and git revision have to be stored with it.
CREATE TABLE evaluation_runs (
    id            UUID PRIMARY KEY,
    label         TEXT        NOT NULL,
    git_sha       TEXT,
    config        JSONB       NOT NULL,
    judge_model   TEXT        NOT NULL,
    -- Changing the parser or its version changes chunk boundaries, which would
    -- make two runs incomparable. Recording the digest lets us detect that.
    corpus_digest TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE evaluation_cases (
    id          UUID PRIMARY KEY,
    run_id      UUID        NOT NULL REFERENCES evaluation_runs (id) ON DELETE CASCADE,
    case_id     TEXT        NOT NULL,
    question    TEXT        NOT NULL,
    user_role   TEXT        NOT NULL,
    category    TEXT        NOT NULL,
    answer      TEXT,
    judged      JSONB,
    latencies   JSONB,
    UNIQUE (run_id, case_id)
);

CREATE INDEX evaluation_runs_created_idx ON evaluation_runs (created_at DESC);
