package com.javagenai.rag.corpus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class CorpusLoader {

    public List<CorpusDocument> load() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:corpus/*.md");
            List<CorpusDocument> documents = new ArrayList<>();
            for (Resource resource : resources) {
                String filename = resource.getFilename() == null ? "document.md" : resource.getFilename();
                String body = resource.getContentAsString(StandardCharsets.UTF_8).strip();
                documents.add(new CorpusDocument(titleFrom(filename, body), body));
            }
            if (documents.isEmpty()) {
                throw new IllegalStateException("No markdown files found under classpath:corpus/");
            }
            documents.sort((left, right) -> left.title().compareToIgnoreCase(right.title()));
            return List.copyOf(documents);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read the corpus", exception);
        }
    }

    private static String titleFrom(String filename, String body) {
        String firstLine = body.lines().findFirst().orElse("").strip();
        if (firstLine.startsWith("#")) {
            return firstLine.replaceFirst("^#+\\s*", "").strip();
        }
        return filename.replaceFirst("\\.md$", "");
    }
}
