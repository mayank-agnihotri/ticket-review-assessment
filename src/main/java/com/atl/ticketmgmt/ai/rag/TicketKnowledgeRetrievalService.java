package com.atl.ticketmgmt.ai.rag;

import com.atl.ticketmgmt.common.config.AppProperties;
import com.atl.ticketmgmt.common.exception.AiServiceUnavailableException;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Similarity search over ingested ticket chunks (used by Phase 8 ask API).
 */
@Service
@ConditionalOnProperty(prefix = "app.rag.vector-store", name = "type", havingValue = "pgvector", matchIfMissing = true)
public class TicketKnowledgeRetrievalService {

    private final VectorStore vectorStore;
    private final AppProperties appProperties;

    public TicketKnowledgeRetrievalService(VectorStore vectorStore, AppProperties appProperties) {
        this.vectorStore = vectorStore;
        this.appProperties = appProperties;
    }

    public List<Document> search(String query) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(appProperties.getRag().getTopK())
                    .similarityThreshold(appProperties.getRag().getSimilarityThreshold())
                    .build();
            return vectorStore.similaritySearch(request);
        } catch (Exception ex) {
            throw new AiServiceUnavailableException("Vector similarity search failed", ex);
        }
    }
}
