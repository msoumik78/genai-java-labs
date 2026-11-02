package com.javagenai.rag.answer;

public record RetrievedPassage(String passageId, String sourceTitle, double score, String text) {
}
