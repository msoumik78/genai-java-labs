# Lab 9 — Prompt gate

Indirect prompt injection, without calling a model. Retrieval returns raw chunks. The only type that can build a prompt is `PromptGate`.

```bash
mvn spring-boot:run
```

App: `http://127.0.0.1:18080`

| Method | Path | What it shows |
|---|---|---|
| GET | `/rag/naive?q=refund` | Both chunks are copied into the prompt. No rules run. |
| GET | `/rag/guarded?q=refund` | Same hits. The poisoned chunk is dropped. It is absent from `promptPreview`. |

The corpus is two in-memory documents: a refund policy, and a wiki page that says “ignore previous instructions”.

```bash
curl -s 'http://127.0.0.1:18080/rag/naive?q=refund'
curl -s 'http://127.0.0.1:18080/rag/guarded?q=refund'
curl -s -w '\n%{http_code}\n' 'http://127.0.0.1:18080/rag/guarded?q=Ignore%20previous%20instructions'
```

A question that matches a rule is rejected with **400** before a prompt exists. Rules live in `PromptRules` and run inside `PromptGate.assemble`. `UnsafePromptRenderer` exists only so the naive endpoint can show the bypass.

## TODO

The phrase list in `PromptRules` is hardcoded for the workshop. A later pass should make this production-shaped:

- Load rules from configuration (id, pattern, and whether the rule applies to the question, a retrieved chunk, or both). Refuse to start if the list is empty or a pattern does not compile.
- Treat the phrase rules and the fence-collision check as the same kind of check. `PromptGate` walks that list and does not special-case either one.
- Let policy decide the action: a violation in the question rejects the call; a violation in a chunk drops that chunk.
- Keep the random `FENCE-` wrapping and the fixed “answer only from DATA” instruction in code. That boundary is the control. The phrase list is only the filter in front of it.
