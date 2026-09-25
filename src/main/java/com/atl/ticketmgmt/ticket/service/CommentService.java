package com.atl.ticketmgmt.ticket.service;

import com.atl.ticketmgmt.ai.ingestion.TicketIngestionTrigger;
import com.atl.ticketmgmt.common.exception.ResourceNotFoundException;
import com.atl.ticketmgmt.ticket.api.TicketMapper;
import com.atl.ticketmgmt.ticket.api.dto.CommentResponse;
import com.atl.ticketmgmt.ticket.api.dto.CreateCommentRequest;
import com.atl.ticketmgmt.ticket.domain.Comment;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.repository.CommentRepository;
import com.atl.ticketmgmt.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    static final String DEFAULT_AUTHOR = "system";

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final TicketMapper ticketMapper;
    private final TicketIngestionTrigger ingestionTrigger;

    public CommentService(
            TicketRepository ticketRepository,
            CommentRepository commentRepository,
            TicketMapper ticketMapper,
            TicketIngestionTrigger ingestionTrigger) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.ticketMapper = ticketMapper;
        this.ingestionTrigger = ingestionTrigger;
    }

    @Transactional
    public CommentResponse addComment(String ticketId, CreateCommentRequest request) {
        Ticket ticket = ticketRepository
                .findByTicketId(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        Comment comment = new Comment(ticket, request.body(), DEFAULT_AUTHOR);
        Comment saved = commentRepository.save(comment);
        ingestionTrigger.onTicketChanged(ticketId);
        return ticketMapper.toCommentResponse(saved);
    }
}
