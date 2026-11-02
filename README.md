# Hybrid RAG (Java)

In-process retrieval over a small Northwind Field Service corpus:

1. Vector search with Ollama `nomic-embed-text` (cosine on normalized vectors).
2. BM25 over the same passages.
3. Reciprocal rank fusion (`1 / (60 + rank)`).
4. Cross-encoder rerank with a local ONNX MiniLM (`ms-marco-MiniLM-L-6-v2`).
5. Answer from Ollama `qwen2.5:7b`, using only the top passages.

## Setup

Java 21. Ollama on `http://localhost:11434`.

```bash
ollama pull nomic-embed-text
ollama pull qwen2.5:7b
chmod +x scripts/download-cross-encoder.sh
./scripts/download-cross-encoder.sh
mvn spring-boot:run
```

App: `http://127.0.0.1:18080`

```bash
curl -sG --data-urlencode "question=Is gasket ZX-4419 a warranty part?" http://127.0.0.1:18080/rag
curl -sG --data-urlencode "question=Can I send back an unopened solvent kit?" http://127.0.0.1:18080/rag
curl -sG --data-urlencode "question=Is the night visit fee waived for priority code HARBOR?" http://127.0.0.1:18080/rag
```

The JSON includes `vectorHits`, `lexicalHits`, `fusedHits`, `rerankedHits`, and `passagesGivenToModel`.

`nomic-embed-text` needs the prefixes `search_document: ` and `search_query: `. The index adds them. They are not shown to the cross-encoder or to Qwen.
