package com.atl.ticketmgmt.ai.support;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * Test double that maps identical text to identical vectors (no Ollama required).
 */
public class DeterministicEmbeddingModel implements EmbeddingModel {

    private final int dimensions;

    public DeterministicEmbeddingModel(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings =
                request.getInstructions().stream().map(text -> new Embedding(vectorFor(text), 0)).toList();
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return vectorFor(document.getText());
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private float[] vectorFor(String text) {
        float[] vector = new float[dimensions];
        int seed = text == null ? 0 : text.hashCode();
        for (int i = 0; i < dimensions; i++) {
            seed = seed * 31 + i;
            vector[i] = (seed % 1000) / 1000.0f;
        }
        return vector;
    }
}
