# Lab 2 — Multi-tenant pgvector (isolation in SQL, not the prompt)

The demo that has to land in 90 minutes: **Acme’s RAG query returns Globex’s M&A memo**, then the same query with `tenant_id` enforced in **Postgres** (predicate or RLS) does not. Telling the model “only use Acme documents” is not the control.

Embeddings are **[BAAI/bge-small-en-v1.5](https://huggingface.co/BAAI/bge-small-en-v1.5)** running **in-process** (ONNX / Spring AI Transformers). No Ollama, no OpenAI. First start downloads ~133MB from Hugging Face and caches it. Weak neighbors (`distance` > `lab.max-distance`, default `0.55`) are dropped so leftover Acme HR docs are not presented as Orion hits.

`/ask/*` returns the context window a chat model would see (no chat runtime required).

## Run

```bash
docker compose up -d
mvn spring-boot:run
```

App: `http://127.0.0.1:18082`

| Method | Path | What it shows |
|---|---|---|
| GET | `/search/naive?tenant=acme&q=PROJECT-ORION` | Vector search, **no tenant filter**. `leakedOtherTenant: true`. |
| GET | `/search/prompt?tenant=acme&q=…` | Same retrieval. Isolation asked of the LLM later — chunks still contain Globex. |
| GET | `/search/sql?tenant=acme&q=…` | `WHERE tenant_id = $1`. Leak gone. |
| GET | `/search/rls?tenant=acme&q=…` | **No** `tenant_id` in SQL. `SET LOCAL ROLE lab_app` + `app.current_tenant` + RLS. |
| GET | `/ask/naive` `/ask/sql` `/ask/prompt` | Same retrieval; `answer` is the stuffed context window. |
| GET | `/lab/docs` | Seeded rows (Acme payroll vs Globex `PROJECT-ORION`). |
| POST | `/lab/reseed` | Rebuild table, RLS policy, embeddings. |

Default leak query (omit `q`): *What is the confidential Northwind acquisition code name and price?*

## Demo (one terminal)

```bash
chmod +x scripts/show-leak.sh
./scripts/show-leak.sh
```

You should see Globex `PROJECT-ORION` / `$4.2B` on **naive** and **prompt** (and a **much smaller** `distance` than Acme). On **sql** and **rls**, Globex is gone; Acme leftovers with `distance` above the cutoff are dropped (`droppedWeakMatches`).

## Why not BGE-M3 or NV-Embed-v2

Those score well on MTEB, but they are the wrong shape for a JVM laptop demo:

- **NV-Embed-v2** is a ~7B Mistral-class model. GPU inference, not ONNX-in-process Spring Boot.
- **BAAI/bge-m3** ONNX is ~2.3GB (or ~570MB quantized) and a multi-output (dense/sparse/ColBERT) graph. Spring AI’s transformer embedder expects a sentence-encoder ONNX export.

**bge-small-en-v1.5** is the same BGE contrastive family, 384-d, ~133MB, CPU-friendly. That is what makes Orion vs payroll distances actually separate.

## Why prompt-side tenant filters fail

Retrieval already stuffed the other tenant into the context window. A system prompt cannot un-read those tokens, and it is not an access-control plane. Architects buy **row metadata + a bind variable / RLS**, not “please ignore Globex.”

The JDBC user is Postgres **superuser**. Superusers **skip RLS** (including `FORCE ROW LEVEL SECURITY`). The RLS path `SET LOCAL ROLE lab_app` (no `BYPASSRLS`) so the policy actually runs. Naive/sql stay on the superuser connection: naive still leaks; sql still uses `WHERE tenant_id = ?`.

## Tests

Needs Docker (Testcontainers + `pgvector/pgvector:pg16`). Tests use a stub embedder so CI does not download ONNX:

```bash
mvn test
```
