package com.javagenai.kb.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javagenai.kb.domain.SourceDocument;
import com.javagenai.kb.domain.StructuredDocument;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Posts a file to a running docling-serve and reads {@code document.json_content}.
 * The service stays up between documents, which is what a batch ingest wants;
 * the CLI parser pays process startup on every file.
 */
@ApplicationScoped
public class DoclingServeParser implements DocumentParser {

    public static final String ID = "docling-serve";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(2);

    private final HttpClient http;
    private final URI convertUrl;
    private final URI healthUrl;
    private final Duration timeout;

    @Inject
    public DoclingServeParser(
            @ConfigProperty(name = "kb.ingestion.docling.base-url", defaultValue = "http://localhost:5001")
                    String baseUrl,
            @ConfigProperty(name = "kb.ingestion.docling.timeout-seconds", defaultValue = "300") long timeoutSeconds) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), baseUrl, timeoutSeconds);
    }

    DoclingServeParser(HttpClient http, String baseUrl, long timeoutSeconds) {
        String root = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = http;
        this.convertUrl = URI.create(root + "/v1/convert/file");
        this.healthUrl = URI.create(root + "/health");
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpResponse<Void> response = http.send(
                    HttpRequest.newBuilder(healthUrl).timeout(HEALTH_TIMEOUT).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public StructuredDocument parse(SourceDocument document) {
        try {
            HttpResponse<String> response = http.send(convertRequest(document), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new DocumentParseException(
                        "Docling serve returned HTTP " + response.statusCode() + " for " + document.id());
            }
            String doclingJson = jsonContent(response.body());
            return DoclingDocumentReader.read(document.id(), document.filename(), doclingJson, ID);
        } catch (IOException e) {
            throw new DocumentParseException("Docling serve failed for " + document.id(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DocumentParseException("Docling serve was interrupted", e);
        }
    }

    /**
     * docling-serve returns {@code { status, document: { json_content } }}.
     * {@code json_content} is the DoclingDocument the reader understands.
     */
    private String jsonContent(String body) throws IOException {
        JsonNode root = JSON.readTree(body);
        String status = root.path("status").asText("");
        if (!status.isEmpty() && !"success".equals(status) && !"partial_success".equals(status)) {
            throw new DocumentParseException("Docling serve status " + status + ": " + root.path("errors"));
        }
        JsonNode content = root.path("document").path("json_content");
        if (content.isMissingNode() || content.isNull()) {
            throw new DocumentParseException("Docling serve response has no document.json_content");
        }
        return content.isTextual() ? content.asText() : content.toString();
    }

    private HttpRequest convertRequest(SourceDocument document) {
        String boundary = "kb-" + UUID.randomUUID();
        byte[] body = multipart(boundary, document);
        return HttpRequest.newBuilder(convertUrl)
                .timeout(timeout)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
    }

    private byte[] multipart(String boundary, SourceDocument document) {
        String filename = document.filename() == null || document.filename().isBlank()
                ? "document.bin"
                : document.filename();
        String mediaType = document.mediaType() == null || document.mediaType().isBlank()
                ? "application/octet-stream"
                : document.mediaType();
        String preamble = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"files\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + mediaType + "\r\n\r\n";
        String formats = "\r\n--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"to_formats\"\r\n\r\n"
                + "json\r\n--" + boundary + "--\r\n";
        byte[] head = preamble.getBytes(StandardCharsets.UTF_8);
        byte[] tail = formats.getBytes(StandardCharsets.UTF_8);
        byte[] content = document.content() == null ? new byte[0] : document.content();
        byte[] all = new byte[head.length + content.length + tail.length];
        System.arraycopy(head, 0, all, 0, head.length);
        System.arraycopy(content, 0, all, head.length, content.length);
        System.arraycopy(tail, 0, all, head.length + content.length, tail.length);
        return all;
    }
}
