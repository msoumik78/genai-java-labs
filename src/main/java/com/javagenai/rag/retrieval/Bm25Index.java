package com.javagenai.rag.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.javagenai.rag.corpus.Passage;

public final class Bm25Index {

    private static final double TERM_FREQUENCY_SATURATION = 1.2;
    private static final double LENGTH_NORMALIZATION = 0.75;

    private final List<Passage> passages;
    private final List<Map<String, Integer>> termFrequencies;
    private final List<Integer> passageLengths;
    private final Map<String, Integer> documentFrequency;
    private final double averagePassageLength;

    public Bm25Index(List<Passage> passages) {
        this.passages = List.copyOf(passages);
        this.termFrequencies = new ArrayList<>();
        this.passageLengths = new ArrayList<>();
        this.documentFrequency = new HashMap<>();
        int totalLength = 0;
        for (Passage passage : this.passages) {
            Map<String, Integer> frequencies = new HashMap<>();
            for (String token : PassageTokenizer.tokens(passage.text())) {
                frequencies.merge(token, 1, Integer::sum);
            }
            termFrequencies.add(frequencies);
            int length = frequencies.values().stream().mapToInt(Integer::intValue).sum();
            passageLengths.add(length);
            totalLength += length;
            for (String term : frequencies.keySet()) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }
        this.averagePassageLength = passages.isEmpty() ? 0 : (double) totalLength / passages.size();
    }

    public List<RankedPassage> search(String query, int limit) {
        List<String> queryTerms = PassageTokenizer.tokens(query).stream().distinct().toList();
        List<RankedPassage> ranked = new ArrayList<>();
        for (int index = 0; index < passages.size(); index++) {
            double score = scorePassage(index, queryTerms);
            if (score > 0) {
                ranked.add(new RankedPassage(passages.get(index), score));
            }
        }
        ranked.sort(Comparator.comparingDouble(RankedPassage::score).reversed());
        return ranked.stream().limit(limit).toList();
    }

    private double scorePassage(int passageIndex, List<String> queryTerms) {
        double score = 0;
        int passageLength = passageLengths.get(passageIndex);
        Map<String, Integer> frequencies = termFrequencies.get(passageIndex);
        for (String term : queryTerms) {
            int frequency = frequencies.getOrDefault(term, 0);
            if (frequency == 0) {
                continue;
            }
            score += inverseDocumentFrequency(term) * saturatedTermFrequency(frequency, passageLength);
        }
        return score;
    }

    private double inverseDocumentFrequency(String term) {
        int passagesContainingTerm = documentFrequency.getOrDefault(term, 0);
        return Math.log(1 + (passages.size() - passagesContainingTerm + 0.5) / (passagesContainingTerm + 0.5));
    }

    private double saturatedTermFrequency(int frequency, int passageLength) {
        double lengthScale = 1 - LENGTH_NORMALIZATION
                + LENGTH_NORMALIZATION * (passageLength / Math.max(averagePassageLength, 1));
        return frequency * (TERM_FREQUENCY_SATURATION + 1)
                / (frequency + TERM_FREQUENCY_SATURATION * lengthScale);
    }
}
