# Lab 13 — Score a small RAG pipeline with spring-ai-ragas

A fixed refund corpus, keyword retrieval, and an Ollama answer. Quality is checked by a Spring Boot test, not by a runtime endpoint.

Java 21. Ollama on `http://localhost:11434`:

```bash
ollama pull qwen2.5:7b
ollama pull nomic-embed-text
```

`qwen2.5:7b` generates the answer and judges faithfulness, response relevancy, and context recall. `nomic-embed-text` is the embedding model response relevancy uses. BLEU compares the answer to a reference and does not call a model.

## Tests

Unit tests do not call Ollama:

```bash
mvn test
```

The RAGAS gate needs both models running. It calls `AskService` for two labeled questions and fails the build when a score is outside its bar:

```bash
mvn test -Pragas
```

| Question | Passing bar |
|---|---|
| Refund arrival time (fact is in the corpus) | Faithfulness ≥ 0.5, response relevancy ≥ 0.7, context recall ≥ 0.9, BLEU ≥ 0.15 |
| Guest wifi password (fact is not in the corpus) | Context recall = 0 and response relevancy < 0.4 |

The BLEU bar is low because a correct short answer (`5 business days`) shares little wording with the full reference sentence. It still fails if the answer stops overlapping that sentence. Faithfulness is 0.5 because this judge only partially credits that short answer. The wifi case does not use faithfulness or BLEU as a gate: a correct refusal scores poorly on both.

```yaml
lab.top-k: 2
spring.ai.ollama.chat.options.model: qwen2.5:7b
spring.ai.ollama.embedding.options.model: nomic-embed-text
```

`mvn test -Pragas` makes several judge requests and takes a few minutes.
