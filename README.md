# Lab 14 — MCP server for enterprise tools

One Spring Boot process exposes three read-only tools on a single streamable HTTP endpoint, `http://localhost:18080/mcp`.

| Tool | System it stands in for |
|---|---|
| `findOrdersByStatus` | SQL (`orders` in H2). Status is `PAID`, `PENDING`, or `SHIPPED`. At most 20 rows. |
| `getCustomer` | Customer REST API at `/internal/customers/{id}` |
| `checkInventory` | Inventory API at `/internal/inventory/{sku}` |

The model cannot send SQL or a URL. Each tool is a fixed operation.

Java 21.

## Run

```bash
mvn spring-boot:run
```

## Call it from this app

`/lab` opens an MCP client against the local `/mcp` endpoint, then lists or calls tools. That is the in-process check.

```bash
curl -s http://localhost:18080/lab/tools

curl -s -X POST http://localhost:18080/lab/call \
  -H 'content-type: application/json' \
  -d '{"name":"findOrdersByStatus","arguments":{"status":"PAID"}}'
```

`mvn test` does the same through the client after the server is listening.

## Call it from outside

Cursor, or any streamable HTTP MCP client, uses the server URL directly:

```json
{
  "mcpServers": {
    "lab14": {
      "url": "http://localhost:18080/mcp"
    }
  }
}
```

The MCP Inspector is the other client that speaks the protocol without writing one:

```bash
npx @modelcontextprotocol/inspector
```

Point it at `http://localhost:18080/mcp` with transport Streamable HTTP. The `/internal/**` paths are the systems behind the tools. They are not the MCP endpoint.
