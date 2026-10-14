package com.javagenai.kb.ingestion;

import com.javagenai.kb.domain.StructuredDocument;
import java.util.List;

/**
 * What gets written to {@code documents}: the parser id and the content digest
 * are pinned with the row so a later run can tell a re-parse from a new file.
 */
public record IngestedDocument(
        String documentId,
        String title,
        String filename,
        List<String> allowedRoles,
        String parser,
        String contentDigest,
        StructuredDocument structured) {

    public IngestedDocument {
        allowedRoles = List.copyOf(allowedRoles == null ? List.of() : allowedRoles);
    }
}
