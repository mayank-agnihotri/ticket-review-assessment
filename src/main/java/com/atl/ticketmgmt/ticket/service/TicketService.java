package com.atl.ticketmgmt.ticket.service;

import com.atl.ticketmgmt.ai.ingestion.TicketIngestionTrigger;
import com.atl.ticketmgmt.common.config.AppProperties;
import com.atl.ticketmgmt.common.exception.ResourceNotFoundException;
import com.atl.ticketmgmt.ticket.api.TicketMapper;
import com.atl.ticketmgmt.ticket.api.dto.CreateTicketRequest;
import com.atl.ticketmgmt.ticket.api.dto.TicketDetailResponse;
import com.atl.ticketmgmt.ticket.api.dto.TicketResponse;
import com.atl.ticketmgmt.ticket.api.dto.UpdateTicketRequest;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import com.atl.ticketmgmt.ticket.repository.TicketPublicIdSequenceRepository;
import com.atl.ticketmgmt.ticket.repository.TicketRepository;
import com.atl.ticketmgmt.ticket.support.TicketPublicIdFormatter;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketPublicIdSequenceRepository sequenceRepository;
    private final TicketMapper ticketMapper;
    private final AppProperties appProperties;
    private final TicketDetailLoader ticketDetailLoader;
    private final TicketIngestionTrigger ingestionTrigger;

    public TicketService(
            TicketRepository ticketRepository,
            TicketPublicIdSequenceRepository sequenceRepository,
            TicketMapper ticketMapper,
            AppProperties appProperties,
            TicketDetailLoader ticketDetailLoader,
            TicketIngestionTrigger ingestionTrigger) {
        this.ticketRepository = ticketRepository;
        this.sequenceRepository = sequenceRepository;
        this.ticketMapper = ticketMapper;
        this.appProperties = appProperties;
        this.ticketDetailLoader = ticketDetailLoader;
        this.ingestionTrigger = ingestionTrigger;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request) {
        String publicId = TicketPublicIdFormatter.format(sequenceRepository.nextValue());
        Ticket ticket = new Ticket(
                publicId,
                request.title(),
                request.description(),
                request.priority(),
                TicketStatus.OPEN);
        ticket.setAssignee(request.assignee());
        ticket.setCategory(request.category());
        Ticket saved = ticketRepository.save(ticket);
        return ticketMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getByTicketId(String ticketId) {
        Ticket ticket = ticketRepository
                .findByTicketId(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
        return ticketDetailLoader.load(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> list(String query, TicketStatus statusFilter) {
        int limit = appProperties.getTickets().getListMax();
        String q = normalizeQuery(query);
        PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ticketRepository
                .findForList(q, statusFilter, page)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    private static String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.trim();
    }

    @Transactional
    public TicketDetailResponse update(String ticketId, UpdateTicketRequest request) {
        Ticket ticket = ticketRepository
                .findByTicketId(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (request.title() != null) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.assignee() != null) {
            ticket.setAssignee(request.assignee());
        }

        Ticket saved = ticketRepository.save(ticket);
        ingestionTrigger.onTicketChanged(ticketId);
        return ticketDetailLoader.load(saved);
    }
}
