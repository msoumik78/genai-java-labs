package com.javagenai.kb.ingestion;

import com.javagenai.kb.domain.SourceDocument;
import com.javagenai.kb.domain.StructuredDocument;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Runs the {@code docling} CLI as a subprocess. No container, no port, no health
 * check: the Python sidecar exists only for the lifetime of the process, which
 * keeps local setup to a single {@code pip install}.
 */
@ApplicationScoped
public class DoclingCliParser implements DocumentParser {

    public static final String ID = "docling-cli";

    private final String command;
    private final long timeoutSeconds;

    @Inject
    public DoclingCliParser(
            @ConfigProperty(name = "kb.ingestion.docling.command", defaultValue = "docling") String command,
            @ConfigProperty(name = "kb.ingestion.docling.timeout-seconds", defaultValue = "300") long timeoutSeconds) {
        this.command = command;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public boolean isAvailable() {
        try {
            Process probe = new ProcessBuilder("which", command).start();
            return probe.waitFor() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public StructuredDocument parse(SourceDocument document) {
        Path workspace = null;
        try {
            workspace = Files.createTempDirectory("kb-docling-");
            Path output = runDocling(document, workspace);
            String json = Files.readString(output);
            return DoclingDocumentReader.read(document.id(), document.filename(), json, ID);
        } catch (IOException e) {
            throw new DocumentParseException("Docling failed for " + document.id(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DocumentParseException("Docling was interrupted", e);
        } finally {
            deleteQuietly(workspace);
        }
    }

    /** Writes the document to a temp file, runs Docling, returns the JSON it produced. */
    private Path runDocling(SourceDocument document, Path workspace) throws IOException, InterruptedException {
        Path input = workspace.resolve(safeName(document.filename()));
        Files.write(input, document.content());
        Process process = new ProcessBuilder(
                        command, "--from", sourceFormat(document),
                        "--to", "json",
                        "--output", workspace.toString(),
                        input.toString())
                .redirectErrorStream(true)
                .start();

        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("Docling timed out after " + timeoutSeconds + "s");
        }
        Path produced = jsonBeside(input);
        if (!produced.toFile().exists()) {
            throw new IOException("Docling produced no JSON. Exit code " + process.exitValue());
        }
        return produced;
    }

    /** Docling names the JSON after the input file, not after our document id. */
    private Path jsonBeside(Path input) {
        String name = input.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        return input.resolveSibling(stem + ".json");
    }

    private String safeName(String filename) {
        if (filename == null || filename.isBlank() || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0) {
            return "document.bin";
        }
        return filename;
    }

    /** Docling keys input formats off the extension, so keep it simple and explicit. */
    private String sourceFormat(SourceDocument document) {
        String name = document.filename() == null ? "" : document.filename().toLowerCase();
        if (name.endsWith(".md")) {
            return "md";
        }
        if (name.endsWith(".html") || name.endsWith(".htm")) {
            return "html";
        }
        return "pdf";
    }

    private void deleteQuietly(Path workspace) {
        if (workspace == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(workspace)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // A leftover temp file is harmless.
                }
            });
        } catch (IOException ignored) {
            // A leftover temp directory is harmless.
        }
    }
}
