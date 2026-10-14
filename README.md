# Lab 7 — Semantic cache (local ONNX embeddings)

A chat call checks a small in-memory cache before Ollama. The question is embedded locally with an int8 ONNX model (`nomic-embed-text-v1.5`). The vector is cut to the first 256 Matryoshka dimensions and L2-normalized. Cosine similarity against recent entries is a dot product. A hit returns the stored answer and never calls the model.

Assumes Ollama on `http://localhost:11434`:

```bash
ollama pull llama3.2:1b
chmod +x scripts/download-model.sh
./scripts/download-model.sh
```

Java 21.

## Run

```bash
mvn spring-boot:run
```

| Method | Path | What it shows |
|---|---|---|
| GET | `/chat?q=What is a semantic cache?` | Embed, search, then Ollama on a miss. |

```bash
curl -s "http://127.0.0.1:18080/chat?q=What%20is%20a%20semantic%20cache%3F"
curl -s "http://127.0.0.1:18080/chat?q=Explain%20semantic%20caching"
curl -s "http://127.0.0.1:18080/chat?q=How%20do%20I%20boil%20pasta%3F"
```

The first call is a miss (`cacheHit: false`) and stores the answer. The second is a paraphrase and should hit. The third should miss. `score` is the best cosine similarity. `embeddingMillis` is the local ONNX call. `totalMillis` on a hit stays close to the embedding time.

Those two phrasings score about `0.86`. "How do I boil pasta?" scores about `0.42` against the first question. `lab.similarity-threshold` defaults to `0.80`, between those two. The cache holds `128` recent vectors. Restart the app after changing it, because the threshold is read at startup.
