package com.javagenai.rag.corpus;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class TextWindowChunker {

    public List<Passage> windows(List<CorpusDocument> documents, int characterLimit, int overlapCharacters) {
        List<Passage> passages = new ArrayList<>();
        for (CorpusDocument document : documents) {
            String text = document.body().replaceAll("\\s+", " ").strip();
            passages.addAll(windowsOf(document.title(), text, characterLimit, overlapCharacters));
        }
        return List.copyOf(passages);
    }

    private static List<Passage> windowsOf(String title, String text, int characterLimit, int overlapCharacters) {
        List<Passage> windows = new ArrayList<>();
        int windowStart = 0;
        int windowNumber = 1;
        while (windowStart < text.length()) {
            int windowEnd = endOnWhitespace(text, windowStart, characterLimit);
            String window = text.substring(windowStart, windowEnd).strip();
            if (!window.isEmpty()) {
                windows.add(new Passage(title + "#" + windowNumber, title, window));
                windowNumber++;
            }
            if (windowEnd >= text.length()) {
                break;
            }
            int nextStart = windowEnd - overlapCharacters;
            if (nextStart <= windowStart) {
                nextStart = windowEnd;
            }
            windowStart = advanceToWhitespace(text, nextStart);
        }
        return windows;
    }

    private static int endOnWhitespace(String text, int windowStart, int characterLimit) {
        int hardEnd = Math.min(windowStart + characterLimit, text.length());
        if (hardEnd >= text.length()) {
            return text.length();
        }
        int lastSpace = text.lastIndexOf(' ', hardEnd);
        if (lastSpace > windowStart) {
            return lastSpace;
        }
        return hardEnd;
    }

    private static int advanceToWhitespace(String text, int index) {
        int cursor = Math.max(0, index);
        while (cursor < text.length() && text.charAt(cursor) != ' ') {
            cursor++;
        }
        while (cursor < text.length() && text.charAt(cursor) == ' ') {
            cursor++;
        }
        return cursor;
    }
}
