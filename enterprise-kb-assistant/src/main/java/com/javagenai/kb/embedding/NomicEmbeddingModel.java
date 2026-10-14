package com.javagenai.kb.embedding;

import com.javagenai.kb.domain.Chunk;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.request.EmbeddingInputType;
import dev.langchain4j.model.embedding.request.EmbeddingRequest;
import dev.langchain4j.model.embedding.response.EmbeddingResponse;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Local, in-process embedding via ONNX. No API key, no per-token cost, and the
 * same weights produce the same vectors on every machine, which is what makes
 * an evaluation run reproducible.
 *
 * <p>Two details that quietly decide retrieval quality: Nomic requires different
 * task prefixes for questions and documents (kept as configuration so they are
 * visible), and Matryoshka truncation lets one embedding pass serve several
 * storage sizes without re-encoding the corpus.
 */
@ApplicationScoped
public class NomicEmbeddingModel implements EmbeddingModel {

    private static final String ONNX_PATH = "onnx/model_quantized.onnx";

    @ConfigProperty(name = "kb.embedding.model-id", defaultValue = "nomic-ai/nomic-embed-text-v1.5")
    String modelId;

    @ConfigProperty(name = "kb.embedding.dimension", defaultValue = "512")
    int dimension;

    @ConfigProperty(name = "kb.embedding.document-prefix", defaultValue = "search_document:")
    String documentPrefix;

    @ConfigProperty(name = "kb.embedding.query-prefix", defaultValue = "search_query:")
    String queryPrefix;

    private volatile OnnxSentenceEncoder encoder;

    /** Embeds a chunk with the document prefix and Matryoshka truncation. */
    public List<Embedding> embedChunks(List<Chunk> chunks) {
        return embedAll(chunks.stream()
                .map(chunk -> TextSegment.from(chunk.textForEmbedding()))
                .toList()).content();
    }

    /** Embeds a question with the query prefix, which is what the corpus text never uses. */
    public Embedding embedQuery(String question) {
        return embed(question, EmbeddingInputType.QUERY);
    }

    private Embedding embed(String text, EmbeddingInputType inputType) {
        return embedOne(text, inputType);
    }

    @Override
    public EmbeddingResponse doEmbed(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        for (String input : request.inputs().stream().map(i -> i.text()).toList()) {
            embeddings.add(embedOne(input, inputTypeOf(request)));
        }
        return EmbeddingResponse.builder().embeddings(embeddings).build();
    }

    private Embedding embedOne(String input, EmbeddingInputType inputType) {
        float[] vector = encoder().encode(prefixed(input, inputType));
        return Embedding.from(Mrl.truncate(vector, dimension));
    }

    /** Nomic's task instructions: without them, recall drops silently. */
    private String prefixed(String text, EmbeddingInputType inputType) {
        boolean isQuery = inputType == EmbeddingInputType.QUERY;
        return (isQuery ? queryPrefix : documentPrefix) + " " + text;
    }

    private EmbeddingInputType inputTypeOf(EmbeddingRequest request) {
        return request.inputType() == null ? EmbeddingInputType.DOCUMENT : request.inputType();
    }

    private OnnxSentenceEncoder encoder() {
        OnnxSentenceEncoder local = encoder;
        if (local == null) {
            synchronized (this) {
                if (encoder == null) {
                    encoder = local = openEncoder();
                }
            }
        }
        return local;
    }

    private OnnxSentenceEncoder openEncoder() {
        try {
            Path model = HubFileCache.download(baseUrl() + ONNX_PATH, modelId.replace('/', '-') + ".onnx");
            Path tokenizer = HubFileCache.download(baseUrl() + "tokenizer.json",
                    modelId.replace('/', '-') + "-tokenizer.json");
            return new OnnxSentenceEncoder(model, tokenizer);
        } catch (IOException e) {
            throw new IllegalStateException("Model download or load failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Model download was interrupted", e);
        }
    }

    private String baseUrl() {
        return "https://huggingface.co/" + modelId + "/resolve/main/";
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public String modelName() {
        return modelId;
    }

    @PreDestroy
    void closeEncoder() {
        OnnxSentenceEncoder local = encoder;
        if (local != null) {
            local.close();
        }
    }
}