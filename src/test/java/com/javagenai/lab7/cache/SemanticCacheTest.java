package com.javagenai.lab7.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.javagenai.lab7.config.LabProperties;
import org.junit.jupiter.api.Test;

class SemanticCacheTest {

    @Test
    void hitsANearDuplicateAndMissesAnUnrelatedVector() {
        SemanticCache cache = cache(0.92, 8);
        cache.store(unit(1, 0), "cached");

        SemanticCache.Match hit = cache.search(unit(0.99f, 0.141067f));
        SemanticCache.Match miss = cache.search(unit(0, 1));

        assertThat(hit.hit()).isTrue();
        assertThat(hit.answer()).isEqualTo("cached");
        assertThat(miss.hit()).isFalse();
    }

    @Test
    void dropsTheOldestEntryPastCapacity() {
        SemanticCache cache = cache(0.99, 1);
        cache.store(unit(1, 0), "first");
        cache.store(unit(0, 1), "second");

        assertThat(cache.search(unit(1, 0)).hit()).isFalse();
        assertThat(cache.search(unit(0, 1)).answer()).isEqualTo("second");
    }

    private static SemanticCache cache(double threshold, int capacity) {
        LabProperties lab = new LabProperties();
        lab.setSimilarityThreshold(threshold);
        lab.setCacheCapacity(capacity);
        return new SemanticCache(lab);
    }

    private static float[] unit(float x, float y) {
        double norm = Math.sqrt(x * x + y * y);
        return new float[] {(float) (x / norm), (float) (y / norm)};
    }
}
