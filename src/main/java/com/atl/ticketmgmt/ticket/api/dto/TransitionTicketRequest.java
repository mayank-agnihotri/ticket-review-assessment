package com.atl.ticketmgmt.ticket.api.dto;

import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionTicketRequest(@NotNull TicketStatus status) {}
