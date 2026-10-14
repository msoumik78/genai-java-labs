package com.javagenai.kb.ingestion;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Picks the parser named by {@code kb.ingestion.parser}. */
@ApplicationScoped
public class ParserSelection {

    private final String name;
    private final DoclingCliParser cli;
    private final DoclingServeParser serve;

    @Inject
    public ParserSelection(
            @ConfigProperty(name = "kb.ingestion.parser", defaultValue = "docling-cli") String name,
            DoclingCliParser cli,
            DoclingServeParser serve) {
        this.name = name;
        this.cli = cli;
        this.serve = serve;
    }

    public DocumentParser selected() {
        return switch (name) {
            case DoclingCliParser.ID -> cli;
            case DoclingServeParser.ID -> serve;
            default -> throw new IllegalArgumentException(
                    "Unknown kb.ingestion.parser '" + name + "'. Use docling-cli or docling-serve.");
        };
    }
}
