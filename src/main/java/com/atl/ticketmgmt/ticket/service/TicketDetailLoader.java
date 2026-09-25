package com.atl.ticketmgmt.ticket.service;

import com.atl.ticketmgmt.ticket.api.TicketMapper;
import com.atl.ticketmgmt.ticket.api.dto.TicketDetailResponse;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.repository.CommentRepository;
import org.springframework.stereotype.Component;

@Component
public class TicketDetailLoader {

    private final CommentRepository commentRepository;
    private final TicketMapper ticketMapper;

    public TicketDetailLoader(CommentRepository commentRepository, TicketMapper ticketMapper) {
        this.commentRepository = commentRepository;
        this.ticketMapper = ticketMapper;
    }

    public TicketDetailResponse load(Ticket ticket) {
        var comments = commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId());
        return ticketMapper.toDetailResponse(ticket, comments);
    }
}
