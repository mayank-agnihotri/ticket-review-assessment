package com.atl.ticketmgmt.ai.ingestion;

import java.util.List;

/**
 * Vector persistence hook for Phase 7. Phase 6 defines the contract only.
 */
public interface TicketKnowledgeVectorStore {

    /**
     * Deletes existing vectors for {@code ticketId} and stores {@code chunks}.
     * When {@code chunks} is empty, only deletion is performed (no embeddings written).
     */
    void replaceForTicket(String ticketId, List<KnowledgeChunk> chunks);
}
