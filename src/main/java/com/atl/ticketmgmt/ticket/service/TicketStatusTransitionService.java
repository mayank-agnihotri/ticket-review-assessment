package com.atl.ticketmgmt.ticket.service;

import com.atl.ticketmgmt.ai.ingestion.TicketIngestionTrigger;
import com.atl.ticketmgmt.common.exception.ResourceNotFoundException;
import com.atl.ticketmgmt.ticket.api.dto.TicketDetailResponse;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.domain.TicketStateMachine;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import com.atl.ticketmgmt.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketStatusTransitionService {

    private final TicketRepository ticketRepository;
    private final TicketDetailLoader ticketDetailLoader;
    private final TicketIngestionTrigger ingestionTrigger;

    public TicketStatusTransitionService(
            TicketRepository ticketRepository,
            TicketDetailLoader ticketDetailLoader,
            TicketIngestionTrigger ingestionTrigger) {
        this.ticketRepository = ticketRepository;
        this.ticketDetailLoader = ticketDetailLoader;
        this.ingestionTrigger = ingestionTrigger;
    }

    @Transactional
    public TicketDetailResponse transition(String ticketId, TicketStatus targetStatus) {
        Ticket ticket = ticketRepository
                .findByTicketId(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        TicketStateMachine.assertTransition(ticket.getStatus(), targetStatus);
        ticket.setStatus(targetStatus);
        Ticket saved = ticketRepository.save(ticket);
        ingestionTrigger.onTicketChanged(ticketId);
        return ticketDetailLoader.load(saved);
    }
}
