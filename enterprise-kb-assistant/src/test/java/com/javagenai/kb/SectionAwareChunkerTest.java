package com.javagenai.kb.chunking;

import static org.assertj.core.api.Assertions.assertThat;

import com.javagenai.kb.domain.Block;
import com.javagenai.kb.domain.Chunk;
import com.javagenai.kb.domain.Section;
import com.javagenai.kb.domain.StructuredDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class SectionAwareChunkerTest {

    private final SectionAwareChunker chunker = new SectionAwareChunker();

    @Test
    void keepsATableInOnePiece() {
        String table = "| Region | Lead time |\n| --- | --- |\n| EU | 5 days |\n| US | 2 days |";

        List<Chunk> chunks = chunker.chunk(
                documentWith(section("s1", "Shipping", Block.table(table))), List.of("EMPLOYEE"));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().text()).contains("EU").contains("US");
    }

    @Test
    void splitsProseAtParagraphBoundaries() {
        String body = "First paragraph about leave.\n\nSecond paragraph about carry over.";

        List<Chunk> chunks = chunker.chunk(
                documentWith(section("s1", "Leave", Block.paragraph(body))), List.of());

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).text()).isEqualTo("First paragraph about leave.");
    }

    @Test
    void keepsTheHeadingPathOnEveryChunk() {
        List<Chunk> chunks = chunker.chunk(
                documentWith(section("s1", "Parental Leave", Block.paragraph("Twelve weeks."))),
                List.of());

        assertThat(chunks.getFirst().sectionPath()).isEqualTo("Parental Leave");
        assertThat(chunks.getFirst().textForEmbedding())
                .isEqualTo("Parental Leave\n\nTwelve weeks.");
    }

    @Test
    void aDocumentWithNoHeadingEmbedsItsTextUnchanged() {
        List<Chunk> chunks = chunker.chunk(
                documentWith(section("s1", "", Block.paragraph("Plain text."))), List.of());

        assertThat(chunks.getFirst().textForEmbedding()).isEqualTo("Plain text.");
    }

    @Test
    void carriesDocumentRolesOntoEveryChunk() {
        List<Chunk> chunks = chunker.chunk(
                documentWith(section("s1", "Comp", Block.paragraph("Salary bands."))),
                List.of("MANAGER"));

        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.allowedRoles()).containsExactly("MANAGER"));
    }

    private StructuredDocument documentWith(Section... sections) {
        return new StructuredDocument("doc-1", "Title", List.of(sections),
                new com.javagenai.kb.domain.ParseStats(1, 500, 1, "test"));
    }

    private Section section(String id, String heading, Block... blocks) {
        return new Section(id, heading, 1, List.of(heading), List.of(blocks));
    }
}
