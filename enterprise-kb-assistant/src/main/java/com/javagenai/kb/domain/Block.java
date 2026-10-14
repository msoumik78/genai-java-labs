package com.javagenai.kb.domain;

/**
 * One unit of content inside a section. Tables stay whole because splitting a
 * table across chunks destroys the answer to most questions asked of it.
 */
public record Block(Kind kind, String text) {

    public enum Kind {
        PARAGRAPH,
        LIST_ITEM,
        TABLE,
        CODE
    }

    public Block {
        if (kind == null) {
            throw new IllegalArgumentException("kind is required");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
    }

    public static Block paragraph(String text) {
        return new Block(Kind.PARAGRAPH, text);
    }

    public static Block table(String markdownTable) {
        return new Block(Kind.TABLE, markdownTable);
    }
}
