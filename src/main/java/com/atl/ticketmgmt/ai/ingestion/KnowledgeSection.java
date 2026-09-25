package com.atl.ticketmgmt.ai.ingestion;

/**
 * A logical section of ticket knowledge before chunking.
 */
public record KnowledgeSection(ChunkSourceType sourceType, String text) {}
