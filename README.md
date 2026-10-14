# Lab 6 — Token usage per tenant (Spring AI + Micrometer)

One chat endpoint against local Ollama. Spring AI already creates a Micrometer observation for `ChatModel`. Token counts are span attributes only, so this lab copies the exact usage onto counters you can aggregate by tenant.

Assumes Ollama on `http://localhost:11434`:

```bash
ollama pull llama3.2:1b
```

Java 21.

## Run

```bash
mvn spring-boot:run
```

| Method | Path | What it shows |
|---|---|---|
| GET | `/chat?q=hello&tenant=acme` | One Ollama call. Response includes this call's input and output tokens. |
| GET | `/usage` | Running totals from the custom meters. |
| GET | `/actuator/prometheus` | Same meters for scrape. Spring AI's chat timer is here too. |

```bash
curl -s "http://127.0.0.1:18080/chat?q=Say%20hello&tenant=acme"
curl -s "http://127.0.0.1:18080/chat?q=Say%20hello&tenant=globex"
curl -s http://127.0.0.1:18080/usage
curl -s http://127.0.0.1:18080/actuator/prometheus | grep genai_chat
```

`genai_chat_tokens_total{tenant,model,token_type}` is the aggregate. `token_type` is `input` or `output`. `genai_chat_calls_total` counts completed chats. The observation filter also adds `tenant` as a low-cardinality tag on the Spring AI chat span.

Keep the tenant set small. A raw user id is fine on a trace, and a poor Prometheus label.

## Docker Compose

Ollama stays on the host (`localhost:11434`). The app container reaches it through `host.docker.internal`. Prometheus scrapes `app:18080/actuator/prometheus` every 5 seconds.

```bash
docker compose up --build
```

App: `http://127.0.0.1:18080`. Prometheus: `http://127.0.0.1:9090`.

```bash
curl -s "http://127.0.0.1:18080/chat?q=Say%20hello&tenant=acme"
curl -s "http://127.0.0.1:18080/chat?q=Say%20hello&tenant=globex"
```

The counter is registered on the first completed chat, so run the curls above before opening the graph. In the Prometheus graph box, this draws one line per tenant and token direction:

```promql
sum by (tenant, token_type) (genai_chat_tokens_total{tenant=~"acme|globex"})
```

The legend is the tenant plus `token_type` (`input` or `output`), so acme and globex stay on separate lines.
