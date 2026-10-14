package com.javagenai.kb.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.javagenai.kb.domain.SourceDocument;
import com.javagenai.kb.domain.StructuredDocument;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DoclingServeParserTest {

    private HttpServer server;
    private String doclingJson;
    private int statusCode = 200;
    private String responseStatus = "success";
    private final AtomicReference<String> posted = new AtomicReference<>();

    @BeforeEach
    void start() throws IOException {
        doclingJson = new String(
                getClass().getResourceAsStream("/docling-policy.json").readAllBytes(), StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", exchange -> {
            byte[] ok = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, ok.length);
            exchange.getResponseBody().write(ok);
            exchange.close();
        });
        server.createContext("/v1/convert/file", exchange -> {
            posted.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = envelope().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void readsJsonContentFromTheServeEnvelope() {
        DoclingServeParser parser = parser();

        StructuredDocument document = parser.parse(source("policy-1"));

        assertThat(parser.isAvailable()).isTrue();
        assertThat(document.title()).isEqualTo("policy");
        assertThat(document.stats().parser()).isEqualTo("docling-serve");
        assertThat(document.stats().tableCount()).isEqualTo(1);
        assertThat(document.allBlocks())
                .extracting(block -> block.text())
                .anyMatch(text -> text.contains("12 weeks"))
                .anyMatch(text -> text.contains("EU") && text.contains("5 days"));
        assertThat(posted.get()).contains("name=\"files\"").contains("name=\"to_formats\"").contains("policy.md");
    }

    @Test
    void pinsParserAndContentDigestOnIngest() {
        byte[] bytes = "same bytes".getBytes(StandardCharsets.UTF_8);
        DocumentIngestor ingestor = new DocumentIngestor(parser());

        IngestedDocument first = ingestor.ingest(source("policy-1", bytes));
        IngestedDocument second = ingestor.ingest(source("policy-1", bytes));

        assertThat(first.parser()).isEqualTo("docling-serve");
        assertThat(first.contentDigest()).hasSize(64).isEqualTo(second.contentDigest());
        assertThat(first.contentDigest()).isEqualTo(CorpusDigest.sha256(bytes));
        assertThat(first.allowedRoles()).containsExactly("EMPLOYEE");
    }

    @Test
    void rejectsAFailedConversion() {
        responseStatus = "failure";

        assertThatThrownBy(() -> parser().parse(source("policy-1")))
                .isInstanceOf(DocumentParseException.class)
                .hasMessageContaining("failure");
    }

    @Test
    void reportsTheServiceDownWhenHealthFails() throws IOException {
        server.stop(0);
        HttpServer dead = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        dead.start();
        int port = dead.getAddress().getPort();
        dead.stop(0);

        DoclingServeParser parser = new DoclingServeParser(
                java.net.http.HttpClient.newHttpClient(), "http://127.0.0.1:" + port, 5);

        assertThat(parser.isAvailable()).isFalse();
    }

    private DoclingServeParser parser() {
        int port = server.getAddress().getPort();
        return new DoclingServeParser(
                java.net.http.HttpClient.newHttpClient(), "http://127.0.0.1:" + port, 5);
    }

    private String envelope() {
        return "{\"status\":\"" + responseStatus + "\",\"document\":{\"json_content\":" + doclingJson + "}}";
    }

    private SourceDocument source(String id) {
        return source(id, "# Leave\n".getBytes(StandardCharsets.UTF_8));
    }

    private SourceDocument source(String id, byte[] bytes) {
        return new SourceDocument(id, "policy.md", "text/markdown", List.of("EMPLOYEE"), bytes);
    }
}
