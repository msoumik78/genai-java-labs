package com.javagenai.lab9.vo;

import java.util.List;

public final class Prompt {

    private final String text;
    private final List<String> admittedChunkIds;
    private final List<DroppedChunk> dropped;

    public Prompt(String text, List<String> admittedChunkIds, List<DroppedChunk> dropped) {
        this.text = text;
        this.admittedChunkIds = List.copyOf(admittedChunkIds);
        this.dropped = List.copyOf(dropped);
    }

    public String text() {
        return text;
    }

    public List<String> admittedChunkIds() {
        return admittedChunkIds;
    }

    public List<DroppedChunk> dropped() {
        return dropped;
    }
}
