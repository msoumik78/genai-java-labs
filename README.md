# Lab 1 — Fence the LLM (Spring AI + Ollama)

Lean Spring Boot app for the Saturday workshop: **a slow model must not starve the rest of the service**, and **you must not retry a completion like a GET**.

Assumes **Ollama** on `http://localhost:11434` with a chat model (default `llama3.2`). Pull if needed:

```bash
ollama pull llama3.2
```

Java 21. No Jlama. Tomcat is capped at **8** threads so a laptop can show the blast radius.

## Run

```bash
mvn spring-boot:run
```

App: `http://127.0.0.1:18080`

| Method | Path | What it shows |
|---|---|---|
| GET | `/orders` | Fast API (1 ms). Must stay fast while chat is busy. |
| GET | `/chat/naive?q=ping` | LLM on the **request thread** (+ `lab.extra-hold-ms`). |
| GET | `/chat/fenced?q=ping` | Same call, max **2** in flight. Extra chats get **429**. |
| GET | `/refund/bad-retry?orderId=A-1` | Side effect **inside** the retry loop → 3 ledger posts. |
| GET | `/refund/idempotent?orderId=A-1` | Side effect **once**, then retry the model. |
| GET | `/lab/stats` | Hold ms, fence size, refund count. |
| POST | `/lab/reset-ledger` | Zero the demo counter. |

`lab.extra-hold-ms` (default **8000**) sleeps **before** Ollama so the demo works even if the model is fast. Set `0` to use only real generation time.

```yaml
lab.extra-hold-ms: 8000
lab.fence-size: 2
spring.ai.ollama.chat.options.model: llama3.2
```

## Demo (two terminals)

**A — orders should stay snappy**

```bash
while true; do curl -s -w " %{time_total}\n" http://127.0.0.1:18080/orders; sleep 0.5; done
```

**B — naive (orders will stall)**

```bash
chmod +x scripts/*.sh
./scripts/storm-chat.sh 10 /chat/naive
```

**B — fenced (orders stay up; extra chat is 429)**

```bash
./scripts/storm-chat.sh 10 /chat/fenced
```

**Retry / double refund**

```bash
curl -s http://127.0.0.1:18080/lab/reset-ledger
curl -s http://127.0.0.1:18080/refund/bad-retry
# refundsPosted = 3
curl -s -X POST http://127.0.0.1:18080/lab/reset-ledger
curl -s http://127.0.0.1:18080/refund/idempotent
# refundsPosted = 1
```

Facilitator timing: [SATURDAY.md](SATURDAY.md).
