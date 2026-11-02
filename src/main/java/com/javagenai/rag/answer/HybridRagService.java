package com.javagenai.rag.answer;

import java.util.List;

import com.javagenai.rag.config.RagSettings;
import com.javagenai.rag.rerank.CrossEncoderReranker;
import com.javagenai.rag.retrieval.PassageIndexes;
import com.javagenai.rag.retrieval.RankedPassage;
import com.javagenai.rag.retrieval.ReciprocalRankFusion;
import org.springframework.stereotype.Service;

@Service
public class HybridRagService {

    private final RagSettings settings;
    private final PassageIndexes indexes;
    private final CrossEncoderReranker crossEncoder;
    private final GroundedAnswerer groundedAnswerer;

    public HybridRagService(
            RagSettings settings,
            PassageIndexes indexes,
            CrossEncoderReranker crossEncoder,
            GroundedAnswerer groundedAnswerer) {
        this.settings = settings;
        this.indexes = indexes;
        this.crossEncoder = crossEncoder;
        this.groundedAnswerer = groundedAnswerer;
    }

    public RagAnswer answer(String question) {
        List<RankedPassage> vectorHits = indexes.vectorHits(question, settings.vectorHitCount());
        List<RankedPassage> lexicalHits = indexes.lexicalHits(question, settings.lexicalHitCount());
        List<RankedPassage> fusedHits = ReciprocalRankFusion.fuse(
                vectorHits,
                lexicalHits,
                settings.reciprocalRankConstant(),
                settings.rerankPoolSize());
        List<RankedPassage> rerankedHits = crossEncoder.rerank(question, fusedHits);
        List<RankedPassage> relevantEnough = rerankedHits.stream()
                .filter(hit -> hit.score() > 0)
                .limit(settings.passagesInPrompt())
                .toList();
        List<RankedPassage> passagesGivenToModel = relevantEnough.isEmpty()
                ? rerankedHits.stream().limit(1).toList()
                : relevantEnough;
        String answer = groundedAnswerer.compose(question, passagesGivenToModel);
        return new RagAnswer(
                question,
                answer,
                toView(vectorHits),
                toView(lexicalHits),
                toView(fusedHits),
                toView(rerankedHits),
                toView(passagesGivenToModel));
    }

    private static List<RetrievedPassage> toView(List<RankedPassage> hits) {
        return hits.stream()
                .map(hit -> new RetrievedPassage(
                        hit.passage().passageId(),
                        hit.passage().sourceTitle(),
                        hit.score(),
                        hit.passage().text()))
                .toList();
    }
}
