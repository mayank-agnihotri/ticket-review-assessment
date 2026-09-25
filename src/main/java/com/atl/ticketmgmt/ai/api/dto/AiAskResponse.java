package com.atl.ticketmgmt.ai.api.dto;

import java.util.List;

public record AiAskResponse(
        String answer,
        List<String> citedTicketIds,
        boolean grounded,
        boolean noMatch,
        AiAskRetrievalInfo retrieval) {}
