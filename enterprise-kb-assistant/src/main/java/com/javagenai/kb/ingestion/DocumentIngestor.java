package com.javagenai.kb.ingestion;

import com.javagenai.kb.domain.SourceDocument;
import com.javagenai.kb.domain.StructuredDocument;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Parses one source and stamps the result with the parser that produced it
 * and a digest of the bytes that went in.
 */
@ApplicationScoped
public class DocumentIngestor {

    private final DocumentParser parser;

    @Inject
    public DocumentIngestor(ParserSelection parsers) {
        this(parsers.selected());
    }

    public DocumentIngestor(DocumentParser parser) {
        this.parser = parser;
    }

    public IngestedDocument ingest(SourceDocument source) {
        StructuredDocument structured = parser.parse(source);
        String title = structured.title() == null || structured.title().isBlank()
                ? source.filename()
                : structured.title();
        return new IngestedDocument(
                source.id(),
                title,
                source.filename(),
                source.allowedRoles(),
                structured.stats().parser(),
                CorpusDigest.sha256(source.content()),
                structured);
    }
}
