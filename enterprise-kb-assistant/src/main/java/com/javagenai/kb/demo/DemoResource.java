package com.javagenai.kb.demo;

import com.javagenai.kb.domain.SourceDocument;
import com.javagenai.kb.generation.GeneratedAnswer;
import com.javagenai.kb.generation.GroundedGenerator;
import com.javagenai.kb.ingestion.IngestedDocument;
import com.javagenai.kb.ingestion.IngestionPipeline;
import com.javagenai.kb.ingestion.IngestionReport;
import com.javagenai.kb.retrieval.HybridRetriever;
import com.javagenai.kb.retrieval.RetrievalResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The demo surface: one ingest endpoint and one ask endpoint whose response
 * is the whole pipeline laid bare — both retrieval arms, fusion, the ACL
 * comparison, and the generated answer with its citations. Everything the
 * dashboard summarises can be inspected here for a single question.
 */
@Path("/demo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DemoResource {

    @Inject
    IngestionPipeline pipeline;

    @Inject
    HybridRetriever retriever;

    @Inject
    GroundedGenerator generator;

    /** Ingests one document; body is JSON with base64 content so binary formats work too. */
    @POST
    @Path("/ingest")
    public IngestionReport ingest(IngestRequest request) {
        SourceDocument source = new SourceDocument(
                request.documentId(),
                request.filename(),
                MediaType.APPLICATION_OCTET_STREAM,
                request.roles(),
                decode(request));
        return pipeline.ingest(source);
    }

    @POST
    @Path("/ask")
    public Trace ask(AskRequest request) {
        long started = System.nanoTime();
        List<RetrievalResult> withAccess = retriever.retrieve(request.question(), request.role(), 5);
        List<RetrievalResult> withoutAccess = retriever.retrieve(request.question(), null, 5);
        long retrievalMs = (System.nanoTime() - started) / 1_000_000;

        GeneratedAnswer answer = generator.answer(request.question(), withAccess);
        return new Trace(
                request.question(),
                request.role(),
                arms(withAccess),
                withoutAccess.size() - withAccess.size(),
                answer,
                retrievalMs);
    }

    /** Per-arm ranks for the retrieved set, so a bad result can be attributed to an arm. */
    private List<ArmHit> arms(List<RetrievalResult> results) {
        return results.stream()
                .map(result -> new ArmHit(
                        result.chunk().id(),
                        result.chunk().sectionPath(),
                        round(result.fusionScore()),
                        result.vectorRank(),
                        result.lexicalRank()))
                .collect(Collectors.toList());
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private static byte[] decode(IngestRequest request) {
        if (request.contentBase64() != null && !request.contentBase64().isBlank()) {
            return Base64.getDecoder().decode(request.contentBase64());
        }
        return (request.text() == null ? "" : request.text()).getBytes(StandardCharsets.UTF_8);
    }

    public record IngestRequest(
            String documentId,
            String filename,
            String text,
            String contentBase64,
            List<String> roles) {

        public IngestRequest {
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }

    public record AskRequest(String question, String role) {
    }

    public record ArmHit(String chunkId, String sectionPath, double fusionScore, int vectorRank, int lexicalRank) {
    }

    public record Trace(
            String question,
            String role,
            List<ArmHit> context,
            int hiddenByAccessControl,
            GeneratedAnswer answer,
            long retrievalMs) {
    }
}