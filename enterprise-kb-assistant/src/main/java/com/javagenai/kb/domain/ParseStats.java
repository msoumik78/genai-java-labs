package com.javagenai.kb.domain;

/**
 * Quality signals from parsing. A near-zero character count means the source had no
 * text layer, and indexing it anyway would quietly depress recall weeks later.
 */
public record ParseStats(int pageCount, int characterCount, int tableCount, String parser) {

    public static ParseStats empty() {
        return new ParseStats(0, 0, 0, "none");
    }

    /** True when the parser produced almost nothing, which usually means a scanned document. */
    public boolean looksScanned() {
        return characterCount < 200;
    }
}