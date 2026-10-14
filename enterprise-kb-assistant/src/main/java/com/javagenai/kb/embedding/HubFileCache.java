package com.javagenai.kb.embedding;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Downloads model artifacts from HuggingFace on first use, then reuses disk.
 * The write is atomic (.part then move) so an interrupted download never leaves
 * a truncated model that would fail at session-load time.
 */
public final class HubFileCache {

    private static final Path CACHE_DIR = Path.of(System.getProperty("user.home"), ".cache", "kb-models");

    private HubFileCache() {
    }

    /** Returns the cached file, downloading it first if missing or suspiciously small. */
    public static Path download(String url, String fileName) throws IOException, InterruptedException {
        Files.createDirectories(CACHE_DIR);
        Path destination = CACHE_DIR.resolve(fileName);
        if (Files.isRegularFile(destination) && Files.size(destination) > 1_000) {
            return destination;
        }
        Path partial = destination.resolveSibling(fileName + ".part");
        fetchInto(url, partial);
        Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    private static void fetchInto(String url, Path target) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "enterprise-kb-assistant")
                .GET()
                .build();
        HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        try (InputStream body = response.body()) {
            Files.copy(body, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
}