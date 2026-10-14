package com.javagenai.kb.domain;

import java.util.List;

/**
 * A headed region of a document, carrying its position in the heading hierarchy.
 * The path is what lets us prefix chunk text with context during embedding.
 */
public record Section(
        String id,
        String heading,
        int level,
        List<String> path,
        List<Block> blocks) {

    public Section {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        path = List.copyOf(path == null ? List.of() : path);
        blocks = List.copyOf(blocks == null ? List.of() : blocks);
    }

    /** "Benefits > Leave > Parental Leave", or "" when the section has no heading. */
    public String pathAsText() {
        return String.join(" > ", path);
    }
}
