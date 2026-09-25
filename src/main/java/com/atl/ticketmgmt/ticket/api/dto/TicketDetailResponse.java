package com.atl.ticketmgmt.ticket.api.dto;

import com.atl.ticketmgmt.ticket.domain.Priority;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import java.time.Instant;
import java.util.List;

public record TicketDetailResponse(
        String ticketId,
        String title,
        String description,
        Priority priority,
        String assignee,
        String category,
        TicketStatus status,
        String resolutionNotes,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> comments) {

    public static TicketDetailResponse from(TicketResponse ticket, List<CommentResponse> comments) {
        return new TicketDetailResponse(
                ticket.ticketId(),
                ticket.title(),
                ticket.description(),
                ticket.priority(),
                ticket.assignee(),
                ticket.category(),
                ticket.status(),
                ticket.resolutionNotes(),
                ticket.createdAt(),
                ticket.updatedAt(),
                comments);
    }
}
