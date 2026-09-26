package com.javagenai.ingest;

import com.javagenai.ingest.pipeline.DocumentIngestor;
import com.javagenai.ingest.store.EmbeddingStoreStrategy;
import com.javagenai.ingest.store.Stores;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;

import java.nio.file.Path;
import java.util.List;

public final class IngestMain {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: ingest <chroma|pinecone|qdrant> <directory>");
            System.exit(2);
        }
        EmbeddingStoreStrategy strategy = Stores.select(args[0]);
        var store = strategy.open();
        DocumentIngestor ingestor = new DocumentIngestor(
                new AllMiniLmL6V2QuantizedEmbeddingModel(),
                DocumentSplitters.recursive(300, 30));
        String query = "where is the refund policy";
        List<String> inserted = ingestor.ingest(Path.of(args[1]), store);
        List<String> retrieved = ingestor.search(store, query, 2);
        System.out.println("cleared " + strategy.name() + ", inserted " + inserted.size() + " segment(s)");
        for (String text : inserted) {
            System.out.println("inserted: " + text);
        }
        System.out.println("query: " + query);
        for (int i = 0; i < retrieved.size(); i++) {
            System.out.println("retrieved " + (i + 1) + ": " + retrieved.get(i));
        }
    }
}
