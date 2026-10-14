package com.javagenai.kb.ingestion;

import com.javagenai.kb.domain.ParseStats;

/** What one ingest produced — shown live in the demo so a silent parse failure is visible. */
public record IngestionReport(
        String documentId,
        String title,
        String parser,
        int chunkCount,
        ParseStats stats) {

    public boolean suspicious() {
        return stats.looksScanned() || chunkCount == 0;
    }
}