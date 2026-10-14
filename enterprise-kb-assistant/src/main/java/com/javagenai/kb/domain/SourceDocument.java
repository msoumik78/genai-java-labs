package com.javagenai.kb.domain;

import java.util.List;

/**
 * A document as it arrives from storage, before any structure has been recovered.
 * This is the hand-off point between the parsers and everything downstream.
 */
public record SourceDocument(
        String id,
        String filename,
        String mediaType,
        List<String> allowedRoles,
        byte[] content) {

    public SourceDocument {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        allowedRoles = List.copyOf(allowedRoles == null ? List.of() : allowedRoles);
    }
}
