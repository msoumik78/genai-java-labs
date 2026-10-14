package com.javagenai.lab7.cache;

import java.util.ArrayDeque;
import java.util.Arrays;

import com.javagenai.lab7.config.LabProperties;
import com.javagenai.lab7.embedding.Vectors;
import org.springframework.stereotype.Component;

@Component
public class SemanticCache {

    public record Match(boolean hit, String answer, double score) {
    }

    private record Entry(float[] vector, String answer) {
    }

    private final double threshold;
    private final int capacity;
    private final ArrayDeque<Entry> entries = new ArrayDeque<>();

    public SemanticCache(LabProperties lab) {
        this.threshold = lab.getSimilarityThreshold();
        this.capacity = lab.getCacheCapacity();
    }

    public synchronized Match search(float[] vector) {
        Entry best = null;
        double bestScore = -1;
        for (Entry entry : entries) {
            double score = Vectors.dot(vector, entry.vector);
            if (score > bestScore) {
                bestScore = score;
                best = entry;
            }
        }
        if (best != null && bestScore >= threshold) {
            return new Match(true, best.answer, bestScore);
        }
        return new Match(false, null, best == null ? 0 : bestScore);
    }

    public synchronized void store(float[] vector, String answer) {
        entries.addLast(new Entry(Arrays.copyOf(vector, vector.length), answer));
        while (entries.size() > capacity) {
            entries.removeFirst();
        }
    }
}
