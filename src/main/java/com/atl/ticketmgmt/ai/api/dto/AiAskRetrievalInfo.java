package com.atl.ticketmgmt.ai.api.dto;

public record AiAskRetrievalInfo(int topK, double similarityThreshold, int chunksUsed) {}
