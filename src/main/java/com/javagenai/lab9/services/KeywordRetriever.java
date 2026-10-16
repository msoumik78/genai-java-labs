package com.javagenai.lab9.services;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.javagenai.lab9.vo.RetrievedChunk;

import org.springframework.stereotype.Service;

@Service
public class KeywordRetriever {

    private final DocumentCorpus corpus;

    public KeywordRetriever(DocumentCorpus corpus) {
        this.corpus = corpus;
    }

    public List<RetrievedChunk> retrieve(String question) {
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
        List<String> terms = Arrays.stream(normalized.split("\\W+"))
                .filter(term -> term.length() >= 4)
                .toList();
        if (terms.isEmpty()) {
            return List.of();
        }
        return corpus.documents().stream()
                .filter(doc -> terms.stream().anyMatch(term -> doc.text().toLowerCase(Locale.ROOT).contains(term)))
                .toList();
    }
}
