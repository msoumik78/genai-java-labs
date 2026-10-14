package com.javagenai.kb.chunking;

import com.javagenai.kb.domain.Block;
import com.javagenai.kb.domain.Chunk;
import com.javagenai.kb.domain.Section;
import com.javagenai.kb.domain.StructuredDocument;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Splits a structured document into chunks along its own structure rather than at
 * a fixed character count. Tables stay whole, and every chunk carries the heading
 * path it came from so embeddings can be disambiguated.
 */
@ApplicationScoped
public class SectionAwareChunker {

    @ConfigProperty(name = "kb.chunking.target-tokens", defaultValue = "400")
    int targetTokens;

    @ConfigProperty(name = "kb.chunking.max-tokens", defaultValue = "600")
    int maxTokens;

    @ConfigProperty(name = "kb.chunking.min-tokens", defaultValue = "40")
    int minTokens;

    public List<Chunk> chunk(StructuredDocument document, List<String> allowedRoles) {
        List<Chunk> chunks = new ArrayList<>();
        for (Section section : document.sections()) {
            chunks.addAll(chunkSection(document, section, allowedRoles));
        }
        return chunks;
    }

    private List<Chunk> chunkSection(
            StructuredDocument document, Section section, List<String> allowedRoles) {
        List<Chunk> chunks = new ArrayList<>();
        for (Block block : section.blocks()) {
            // A table answers questions only as a whole, so it is never split.
            if (block.kind() == Block.Kind.TABLE) {
                chunks.add(chunkOf(document, section, block.text(), allowedRoles));
                continue;
            }
            chunks.addAll(chunkProse(document, section, block.text(), allowedRoles));
        }
        return chunks;
    }

    private List<Chunk> chunkProse(
            StructuredDocument document, Section section, String text, List<String> allowedRoles) {
        List<Chunk> chunks = new ArrayList<>();
        for (String paragraph : text.split("\n{2,}")) {
            String trimmed = paragraph.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            chunks.add(chunkOf(document, section, trimmed, allowedRoles));
        }
        return chunks;
    }

    private Chunk chunkOf(
            StructuredDocument document, Section section, String text, List<String> allowedRoles) {
        String id = document.documentId() + ":" + section.id();
        return new Chunk(id, document.documentId(), section.id(), text,
                section.pathAsText(), allowedRoles);
    }

    int targetTokens() {
        return targetTokens;
    }

    int maxTokens() {
        return maxTokens;
    }

    int minTokens() {
        return minTokens;
    }
}