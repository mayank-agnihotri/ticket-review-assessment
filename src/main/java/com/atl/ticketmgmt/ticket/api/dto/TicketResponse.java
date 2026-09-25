package com.atl.ticketmgmt.ticket.api.dto;

import com.atl.ticketmgmt.ticket.domain.Priority;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import java.time.Instant;

public record TicketResponse(
        String ticketId,
        String title,
        String description,
        Priority priority,
        String assignee,
        String category,
        TicketStatus status,
        String resolutionNotes,
        Instant createdAt,
        Instant updatedAt) {
}
