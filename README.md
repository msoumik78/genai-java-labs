# Document ingest

Plain Java. One pipeline loads, splits, and embeds with the in-process MiniLM model (`all-MiniLM-L6-v2`, quantized ONNX). The vector store is a strategy: Chroma, Pinecone, or Qdrant.

Each run clears that store, inserts the files in `docs/`, and prints the inserted segments plus the top 2 matches for "where is the refund policy".

Java 21. From the project root:

```bash
mvn test
```

## Qdrant

Qdrant is not started by this program. Start one container (REST 6333, gRPC 6334). The program uses gRPC.

```bash
docker run -d --name qdrant -p 6333:6333 -p 6334:6334 qdrant/qdrant
mvn -q exec:java -Dexec.args="qdrant docs"
```

Optional environment variables: `QDRANT_HOST` (default `localhost`), `QDRANT_PORT` (default `6334`), `QDRANT_COLLECTION` (default `docs`).

## Chroma

```bash
docker run -d --name chroma -p 8000:8000 chromadb/chroma
mvn -q exec:java -Dexec.args="chroma docs"
```

Optional: `CHROMA_URL` (default `http://localhost:8000`), `CHROMA_COLLECTION` (default `docs`).

## Pinecone

```bash
export PINECONE_API_KEY=...
mvn -q exec:java -Dexec.args="pinecone docs"
```

Optional: `PINECONE_INDEX` (default `docs`). The index must be 384-dimensional, matching MiniLM.
