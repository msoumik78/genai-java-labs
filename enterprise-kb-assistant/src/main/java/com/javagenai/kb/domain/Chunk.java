package com.javagenai.kb.domain;

import java.util.List;

/**
 * A retrievable unit. Chunks are what we embed and what retrieval ranks;
 * the parent section is what the model finally reads.
 */
public record Chunk(
        String id,
        String documentId,
        String sectionId,
        String text,
        String sectionPath,
        List<String> allowedRoles) {

    public Chunk {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        allowedRoles = List.copyOf(allowedRoles == null ? List.of() : allowedRoles);
    }

    /**
     * What gets embedded. Prepending the heading path disambiguates text that
     * repeats across sections, which is common in policy documents.
     */
    public String textForEmbedding() {
        return sectionPath == null || sectionPath.isBlank() ? text : sectionPath + "\n\n" + text;
    }

    public boolean isVisibleTo(String role) {
        return allowedRoles.isEmpty() || allowedRoles.contains(role);
    }
}
