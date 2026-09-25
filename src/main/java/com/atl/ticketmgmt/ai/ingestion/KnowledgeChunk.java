package com.atl.ticketmgmt.ai.ingestion;

import com.atl.ticketmgmt.ticket.domain.Priority;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;

/**
 * Logical chunk ready for embedding and PGVector storage (Phase 7).
 */
public record KnowledgeChunk(
        String content,
        String ticketId,
        TicketStatus status,
        Priority priority,
        String assignee,
        String category,
        int chunkIndex,
        ChunkSourceType sourceType) {}
