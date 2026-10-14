package com.javagenai.kb.domain;

import java.util.List;

/**
 * What a parser recovered about a document: a tree of sections, each holding blocks
 * of content. Recovering this structure is the whole reason the parser exists.
 */
public record StructuredDocument(
        String documentId,
        String title,
        List<Section> sections,
        ParseStats stats) {

    public StructuredDocument {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId is required");
        }
        sections = List.copyOf(sections == null ? List.of() : sections);
        stats = stats == null ? ParseStats.empty() : stats;
    }

    /** Every block in the document, flattened in reading order. */
    public List<Block> allBlocks() {
        return sections.stream().flatMap(section -> section.blocks().stream()).toList();
    }
}
