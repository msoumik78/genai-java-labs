package com.javagenai.kb.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads the committed golden dataset: a static asset, so eval inputs never drift silently. */
public final class GoldenDataset {

    /**
     * The JSONL uses snake_case (the convention for dataset files shared with
     * Python tooling); the records are idiomatic camelCase Java.
     */
    private static final ObjectMapper JSON = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private GoldenDataset() {
    }

    public static List<GoldenCase> load(Path jsonl) {
        try {
            List<GoldenCase> cases = new ArrayList<>();
            for (String line : Files.readAllLines(jsonl)) {
                if (line.isBlank()) {
                    continue;
                }
                cases.add(JSON.readValue(line, GoldenCase.class));
            }
            if (cases.isEmpty()) {
                throw new IllegalStateException("Golden dataset is empty: " + jsonl);
            }
            return List.copyOf(cases);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read golden dataset " + jsonl, e);
        }
    }
}