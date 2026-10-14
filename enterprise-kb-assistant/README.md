# Enterprise Knowledge Assistant

A production-shaped RAG system where **100% of the runtime is Java**: Quarkus +
LangChain4j, local ONNX embeddings and reranking, Postgres for hybrid search and
access control, and DeepSeek for generation and judging. The only Python process
is Docling, a stateless document parser at the edge.

**The point is not the chatbot — it's the evaluation harness.** Every tuning step
is an experiment with before/after numbers on a dashboard:

> "Faithfulness went from 71% to 89%, and here is exactly which step moved it."

## Quickstart

### Docker (recommended)

```bash
cp .env.example .env            # add your DEEPSEEK_API_KEY
docker compose up --build
```

### Local development

```bash
docker compose up -d postgres    # just the database
source ~/softwares/setJVM.sh
mvn quarkus:dev                  # app on :8080, Flyway migrates on boot
```

Requires: Java 21, Postgres 17 + pgvector, `docling` (`uv tool install docling --python 3.10`)
for the CLI parser. ONNX models download on first use into `~/.cache/kb-models`.

## The demo in five calls

```bash
# 1. INGEST — parse (Docling) → chunk (section-aware) → embed (ONNX) → store
curl -s localhost:8080/demo/ingest -H 'content-type: application/json' -d '{
  "documentId": "parental-leave", "filename": "parental-leave.md",
  "text": "# Parental Leave Policy\n\nEmployees receive...", "roles": []}'

# 2. ASK — retrieval arms, RRF fusion, rerank, ACL comparison, grounded answer
curl -s localhost:8080/demo/ask -H 'content-type: application/json' \
  -d '{"question":"How long is parental leave for a second parent?","role":"EMPLOYEE"}'

# 3. ACL — same question as a role that cannot see compensation
curl -s localhost:8080/demo/ask -H 'content-type: application/json' \
  -d '{"question":"What is the Senior engineer salary band?","role":"EMPLOYEE"}'
# → answer is an abstention; hiddenByAccessControl > 0

# 4. EVAL — run the golden dataset, pin an immutable experiment row
curl -s -X POST localhost:8080/eval/run -H 'content-type: application/json' \
  -d '{"label":"r6-full","gitSha":"HEAD"}'

# 5. DASHBOARD — tier metrics, baseline-vs-candidate deltas, scatter
open http://localhost:8080/dashboard
```

## Architecture

```
corpus ─▶ Docling (CLI or serve) ─▶ section-aware chunker ─▶ ONNX embedder ─▶ Postgres
                                                                          (vector + FTS + ACL)
question ─▶ embed ─▶ vector arm ┐
question ─▶ BM25  ─▶ lexical  ─┴▶ RRF fusion ─▶ ONNX cross-encoder rerank ─▶ top-5
                                                                          │
                                          ACL filter inside SQL (pre-ranking)
                                                                          ▼
                                              DeepSeek-V4-Pro grounded answer + citations
                                                                          ▼
                        golden.jsonl ─▶ eval runner ─▶ Tier 1 metrics + DeepSeek-Flash judge
                                                                          ▼
                                                            dashboard (deltas + scatter)
```

### Why these choices

| Decision | Reason |
|---|---|
| **LangChain4j + Quarkus** | Leanest runtime (fast-jar), first-class `Scorer`/reranking abstractions, Red Hat-maintained extension |
| **Local ONNX (nomic-embed + bge-reranker)** | Zero marginal cost, reproducible vectors, MRL dimension sweep without re-embedding |
| **Hybrid vector + BM25, fused by RRF** | Cosine and BM25 aren't comparable; ranks are the shared signal |
| **ACL in SQL `WHERE`** | Post-filtering leaks restricted documents through result counts |
| **Judge ≠ generator model** | The judge never grades its own homework |
| **3-tier metrics** | Tier 1 (recall, phrase hits, citations, ACL) is deterministic and can't drift; Tier 2 is LLM-judged; judge calibration makes Tier 2 credible |
| **Docling at the edge only** | Stateless parse service; everything after parsing is Java |

## Metric tiers

- **Tier 1 — deterministic**: document-level context recall, phrase hits, citation
  validity, abstention behavior. These never change without a pipeline change.
- **Tier 2 — LLM-judged** (`deepseek-flash`): faithfulness (claim-level SUPPORTED /
  UNSUPPORTED / CONTRADICTED), relevancy, completeness.
- **Calibration**: judge self-consistency and human-agreement checks (planned) keep
  Tier 2 honest.

## Repository layout

```
corpus/                     demo documents (roles enforced per document)
eval/dataset/golden.jsonl   33 graded cases: factual, multi-hop, table_lookup,
                            unanswerable, acl_denied
src/main/java/com/javagenai/kb/
  domain/     Chunk, Section, Block, ...
  ingestion/  Docling CLI + serve parsers, pipeline
  chunking/   section-aware chunker
  embedding/  ONNX embedder, MRL truncation
  retrieval/  hybrid store, RRF, cross-encoder reranker
  generation/ grounded generator, DeepSeek client, citation check
  judge/      faithfulness judge
  eval/       runner, store, dashboard
  demo/       trace-style ingest/ask endpoints
Dockerfile, docker-compose.yml
```

## Tests

```bash
mvn test          # 37 unit tests, includes a live ONNX embedding test
```

## Known gaps (documented, not hidden)

- Scanned PDFs (no text layer) are rejected with a warning — no OCR yet
- Synthetic golden set (33 cases); RAGAS testset generation can scale it
- No bootstrap CIs yet on dashboard deltas
- SPLADE/ColBERT retrieval not evaluated; hybrid+rerank covers most of the gap