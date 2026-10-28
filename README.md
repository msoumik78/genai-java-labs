# Lab 11 — A2A (Spring AI)

Two separate Maven projects. The facts server publishes an Agent Card. The host learns the agent from that card and sends a JSON-RPC message. They do not call each other in-process.

Ollama on `http://localhost:11434` with `qwen2.5:7b`:

```bash
ollama pull qwen2.5:7b
```

Java 21. Spring Boot 4.0.6 and Spring AI 2.0.0, because `spring-ai-a2a-server` 0.3.0 is built for that line.

## Server — order facts

```bash
cd a2a-server
mvn spring-boot:run
```

`http://127.0.0.1:18081`

| Method | Path | What it shows |
|---|---|---|
| GET | `/.well-known/agent-card.json` | Agent Card (name, skill `order_facts`, URL) |
| GET | `/card` | Same card |
| POST | `/` | JSON-RPC `message/send` (used by the client) |

Known orders: `A-100` SHIPPED, `B-200` PACKED, `C-300` CANCELLED.

## Client — host

Start the server first so discovery succeeds at startup. If the client starts first, call `/discover` after the server is up.

```bash
cd a2a-client
mvn spring-boot:run
```

`http://127.0.0.1:18082`

| Method | Path | What it shows |
|---|---|---|
| GET | `/discover` | Fetches the remote Agent Card |
| GET | `/ask?q=What is the status of order A-100?` | Host model delegates over A2A |

```bash
curl -s http://127.0.0.1:18081/.well-known/agent-card.json
curl -s http://127.0.0.1:18082/discover
curl -s "http://127.0.0.1:18082/ask?q=What%20is%20the%20status%20of%20order%20A-100%3F"
```

The host is configured with only `remote.agent.url`. The agent name and skills come from the card.
