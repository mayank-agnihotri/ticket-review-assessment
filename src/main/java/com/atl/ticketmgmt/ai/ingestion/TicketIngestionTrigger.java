package com.atl.ticketmgmt.ai.ingestion;

import org.springframework.stereotype.Component;

@Component
public class TicketIngestionTrigger {

    private final TicketIngestionService ticketIngestionService;

    public TicketIngestionTrigger(TicketIngestionService ticketIngestionService) {
        this.ticketIngestionService = ticketIngestionService;
    }

    public void onTicketChanged(String ticketId) {
        ticketIngestionService.scheduleIngest(ticketId);
    }
}
