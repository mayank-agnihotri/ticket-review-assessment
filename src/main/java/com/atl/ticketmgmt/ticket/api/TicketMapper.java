package com.atl.ticketmgmt.ticket.api;

import com.atl.ticketmgmt.ticket.api.dto.CommentResponse;
import com.atl.ticketmgmt.ticket.api.dto.TicketDetailResponse;
import com.atl.ticketmgmt.ticket.api.dto.TicketResponse;
import com.atl.ticketmgmt.ticket.domain.Comment;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getTicketId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCategory(),
                ticket.getStatus(),
                ticket.getResolutionNotes(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }

    public TicketDetailResponse toDetailResponse(Ticket ticket, List<Comment> comments) {
        List<CommentResponse> commentResponses =
                comments.stream().map(this::toCommentResponse).toList();
        return TicketDetailResponse.from(toResponse(ticket), commentResponses);
    }

    public CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getBody(),
                comment.getAuthor(),
                comment.getCreatedAt());
    }
}
