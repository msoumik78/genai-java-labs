package com.javagenai.lab13.retrieval;

import com.javagenai.lab13.corpus.RefundCorpus;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class KeywordRetriever {

    private static final Pattern TOKEN = Pattern.compile("[a-z0-9]+");

    public List<String> retrieve(String query, int topK) {
        Set<String> queryTerms = tokens(query);
        return RefundCorpus.PASSAGES.stream()
                .sorted(Comparator
                        .comparingInt((String passage) -> overlap(queryTerms, passage))
                        .reversed()
                        .thenComparing(RefundCorpus.PASSAGES::indexOf))
                .limit(Math.max(topK, 0))
                .toList();
    }

    static int overlap(Set<String> queryTerms, String passage) {
        Set<String> passageTerms = tokens(passage);
        int score = 0;
        for (String term : queryTerms) {
            if (passageTerms.contains(term)) {
                score++;
            }
        }
        return score;
    }

    static Set<String> tokens(String text) {
        return TOKEN.matcher(text.toLowerCase(Locale.ROOT))
                .results()
                .map(match -> match.group())
                .filter(term -> term.length() > 2)
                .collect(Collectors.toSet());
    }
}
