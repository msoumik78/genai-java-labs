package com.javagenai.kb.ingestion;

import com.javagenai.kb.domain.SourceDocument;
import com.javagenai.kb.domain.StructuredDocument;

/**
 * Recovers document structure from raw bytes. Implementations differ in fidelity:
 * a real layout model understands multi-page tables, a plain text extractor does not.
 */
public interface DocumentParser {

    StructuredDocument parse(SourceDocument document);

    /** True when this parser is configured and ready to run. */
    boolean isAvailable();
}
