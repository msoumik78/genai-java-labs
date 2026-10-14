package com.javagenai.kb.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javagenai.kb.domain.Block;
import com.javagenai.kb.domain.ParseStats;
import com.javagenai.kb.domain.Section;
import com.javagenai.kb.domain.StructuredDocument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads Docling's native JSON into our document model, following the body's
 * reference list so reading order is preserved. Shared by the CLI and HTTP
 * parsers: the mapping is the contract, the transport is an implementation
 * detail.
 *
 * <p>Two schema facts that are easy to get wrong: prose is split into
 * formatting runs (bold text is its own item) that must be reassembled via
 * their parent group, and tables store a nested grid of cell objects rather
 * than a flat cell list.
 */
public final class DoclingDocumentReader {

    private static final ObjectMapper JSON = new ObjectMapper();

    private DoclingDocumentReader() {
    }

    public static StructuredDocument read(
            String documentId, String fallbackTitle, String doclingJson, String parserId) throws IOException {
        JsonNode root = JSON.readTree(doclingJson);
        // Callers hand us the DoclingDocument itself. The serve client unwraps
        // json_content before this; the CLI writes the document at the root.
        Index index = new Index(root);
        List<Section> sections = sectionsOf(documentId, root, index);
        return new StructuredDocument(
                documentId,
                titleOf(root, fallbackTitle),
                sections,
                statsOf(root, sections, parserId));
    }

    private static String titleOf(JsonNode doc, String fallback) {
        String title = doc.path("name").asText("");
        return title.isBlank() ? fallback : title;
    }

    /** Walks body children in order; unresolvable refs are skipped, never guessed. */
    private static List<Section> sectionsOf(String documentId, JsonNode doc, Index index) {
        List<Section> sections = new ArrayList<>();
        for (JsonNode ref : doc.path("body").path("children")) {
            JsonNode item = index.resolve(ref.path("$ref").asText(""));
            if (item == null) {
                continue;
            }
            String selfRef = item.path("self_ref").asText("");
            switch (item.path("label").asText("")) {
                case "title", "section_header" -> sections.add(headingSection(selfRef, item));
                case "inline" -> groupSection(selfRef, item, index).ifPresent(sections::add);
                case "table" -> sections.add(tableSection(documentId, selfRef, item, sections.size()));
                case "text" -> textSection(selfRef, item).ifPresent(sections::add);
                default -> textSection(selfRef, item).ifPresent(sections::add);
            }
        }
        return sections;
    }

    private static Section headingSection(String selfRef, JsonNode item) {
        String heading = item.path("text").asText("").strip();
        return new Section(selfRef, heading, headingLevel(item), List.of(heading),
                List.of(Block.paragraph(heading)));
    }

    private static int headingLevel(JsonNode item) {
        return Math.max(1, item.path("level").asInt(1));
    }

    private static Section tableSection(String documentId, String selfRef, JsonNode item, int index) {
        String id = selfRef.isBlank() ? documentId + "-t" + index : selfRef;
        return new Section(id, "Table", 1, List.of("Table"), List.of(Block.table(renderTable(item))));
    }

    private static Optional<Section> textSection(String selfRef, JsonNode item) {
        String text = item.path("text").asText("");
        if (text.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Section(selfRef, "", 1, List.of(), List.of(Block.paragraph(text.strip()))));
    }

    /** A paragraph group's formatting runs are separate items; concatenate them back together. */
    private static Optional<Section> groupSection(String selfRef, JsonNode group, Index index) {
        StringBuilder paragraph = new StringBuilder();
        for (JsonNode ref : group.path("children")) {
            JsonNode run = index.resolve(ref.path("$ref").asText(""));
            if (run != null) {
                paragraph.append(run.path("text").asText(""));
            }
        }
        String text = paragraph.toString().strip();
        if (text.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Section(selfRef, "", 1, List.of(), List.of(Block.paragraph(text))));
    }

    /** Renders Docling's grid of cell objects as a Markdown table with a header separator. */
    private static String renderTable(JsonNode table) {
        JsonNode grid = table.path("data").path("grid");
        if (grid.isEmpty()) {
            return "";
        }
        StringBuilder markdown = new StringBuilder();
        for (int row = 0; row < grid.size(); row++) {
            JsonNode cells = grid.get(row);
            markdown.append('|');
            for (JsonNode cell : cells) {
                markdown.append(' ').append(escape(cell.path("text").asText(""))).append(" |");
            }
            markdown.append('\n');
            if (row == 0) {
                for (int column = 0; column < cells.size(); column++) {
                    markdown.append("| --- ");
                }
                markdown.append("|\n");
            }
        }
        return markdown.toString();
    }

    private static String escape(String text) {
        return text.replace("|", "\\|").replace("\n", " ");
    }

    private static int pageCount(JsonNode doc) {
        JsonNode pages = doc.path("pages");
        return pages.isArray() || pages.isObject() ? pages.size() : 0;
    }

    private static ParseStats statsOf(JsonNode doc, List<Section> sections, String parserId) {
        int characters = sections.stream()
                .flatMap(section -> section.blocks().stream())
                .mapToInt(block -> block.text().length())
                .sum();
        return new ParseStats(pageCount(doc), characters, doc.path("tables").size(), parserId);
    }

    /** Resolves "#/texts/0" style refs against every top-level collection Docling uses. */
    private record Index(Map<String, JsonNode> byRef) {

        private Index(JsonNode doc) {
            this(build(doc));
        }

        private JsonNode resolve(String ref) {
            return byRef.get(ref);
        }

        private static Map<String, JsonNode> build(JsonNode doc) {
            Map<String, JsonNode> byRef = new HashMap<>();
            for (String collection : List.of("texts", "tables", "groups", "pictures", "key_value_items")) {
                for (JsonNode item : doc.path(collection)) {
                    byRef.put(item.path("self_ref").asText(""), item);
                }
            }
            return byRef;
        }
    }
}