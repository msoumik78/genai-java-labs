package com.javagenai.rag.retrieval;

import java.util.List;

import com.javagenai.rag.config.RagSettings;
import com.javagenai.rag.corpus.CorpusLoader;
import com.javagenai.rag.corpus.Passage;
import com.javagenai.rag.corpus.TextWindowChunker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

@Component
public class PassageIndexes {

    private static final Logger log = LoggerFactory.getLogger(PassageIndexes.class);

    private final List<Passage> passages;
    private final Bm25Index lexicalIndex;
    private final InMemoryVectorIndex vectorIndex;

    public PassageIndexes(
            RagSettings settings,
            CorpusLoader corpusLoader,
            TextWindowChunker chunker,
            EmbeddingModel embeddingModel) {
        this.passages = chunker.windows(
                corpusLoader.load(),
                settings.passageCharacterLimit(),
                settings.passageOverlapCharacters());
        this.lexicalIndex = new Bm25Index(passages);
        this.vectorIndex = InMemoryVectorIndex.build(embeddingModel, passages);
        log.info("Indexed {} passages with BM25 and nomic-embed-text", passages.size());
    }

    public List<Passage> passages() {
        return passages;
    }

    public List<RankedPassage> lexicalHits(String question, int limit) {
        return lexicalIndex.search(question, limit);
    }

    public List<RankedPassage> vectorHits(String question, int limit) {
        return vectorIndex.search(question, limit);
    }
}
