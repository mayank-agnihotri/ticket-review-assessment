package com.atl.ticketmgmt.ticket.api.dto;

import com.atl.ticketmgmt.ticket.domain.Priority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @Size(max = 200) String title,
        @Size(max = 5000) String description,
        Priority priority,
        @Size(max = 200) String assignee) {

    @AssertTrue(message = "At least one field must be provided")
    public boolean isAtLeastOneFieldPresent() {
        return title != null || description != null || priority != null || assignee != null;
    }
}
