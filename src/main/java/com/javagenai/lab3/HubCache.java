package com.javagenai.lab3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/** First run downloads Hub files into ~/.cache/lab3-hf; later runs reuse disk. */
public final class HubCache {

    private static final Path DIR = Path.of(System.getProperty("user.home"), ".cache", "lab3-hf");

    private HubCache() {
    }

    public static Path download(String url, String fileName) throws IOException, InterruptedException {
        Files.createDirectories(DIR);
        Path dest = DIR.resolve(fileName);
        if (Files.isRegularFile(dest) && Files.size(dest) > 1_000) {
            System.out.println("cache hit  " + dest);
            return dest;
        }
        System.out.println("download   " + url);
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "lab3-hf-pipelines")
                .GET()
                .build();
        HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + res.statusCode() + " for " + url);
        }
        Path tmp = dest.resolveSibling(fileName + ".part");
        try (InputStream in = res.body()) {
            Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("saved      " + dest + " (" + Files.size(dest) + " bytes)");
        return dest;
    }
}
