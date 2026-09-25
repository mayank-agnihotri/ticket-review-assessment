package com.atl.ticketmgmt.ticket.api.dto;

import com.atl.ticketmgmt.ticket.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotNull Priority priority,
        @Size(max = 200) String assignee,
        @Size(max = 100) String category) {
}
