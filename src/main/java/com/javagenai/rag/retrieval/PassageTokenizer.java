package com.javagenai.rag.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class PassageTokenizer {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "of", "to", "and", "or", "for", "in", "on", "is", "are",
            "was", "were", "be", "it", "this", "that", "with", "as", "at", "by", "from");

    private PassageTokenizer() {
    }

    static List<String> tokens(String text) {
        String[] pieces = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        List<String> tokens = new ArrayList<>();
        for (String piece : pieces) {
            if (piece.length() > 1 && !STOP_WORDS.contains(piece)) {
                tokens.add(piece);
            }
        }
        return tokens;
    }
}
