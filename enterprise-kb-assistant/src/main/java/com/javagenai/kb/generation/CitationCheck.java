package com.javagenai.kb.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [n] markers are 1-based positions in the context that was shown to the model.
 * A marker is valid only when that position exists.
 */
public final class CitationCheck {

    private static final Pattern MARKER = Pattern.compile("\\[(\\d+)]");

    private CitationCheck() {
    }

    public static Result check(String answer, List<String> chunkIdsInOrder) {
        List<String> ids = chunkIdsInOrder == null ? List.of() : chunkIdsInOrder;
        List<String> cited = new ArrayList<>();
        Matcher matcher = MARKER.matcher(answer == null ? "" : answer);
        boolean valid = true;
        while (matcher.find()) {
            int position = Integer.parseInt(matcher.group(1));
            if (position < 1 || position > ids.size()) {
                valid = false;
                continue;
            }
            cited.add(ids.get(position - 1));
        }
        if (cited.isEmpty()) {
            valid = false;
        }
        return new Result(valid, List.copyOf(cited));
    }

    public record Result(boolean valid, List<String> chunkIds) {
    }
}
