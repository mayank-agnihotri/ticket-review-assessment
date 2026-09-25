package com.atl.ticketmgmt.ticket.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(UUID id, String body, String author, Instant createdAt) {
}
