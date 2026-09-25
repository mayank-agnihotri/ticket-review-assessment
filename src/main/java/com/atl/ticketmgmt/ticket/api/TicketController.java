package com.atl.ticketmgmt.ticket.api;

import com.atl.ticketmgmt.ticket.api.dto.CommentResponse;
import com.atl.ticketmgmt.ticket.api.dto.CreateCommentRequest;
import com.atl.ticketmgmt.ticket.api.dto.CreateTicketRequest;
import com.atl.ticketmgmt.ticket.api.dto.TicketDetailResponse;
import com.atl.ticketmgmt.ticket.api.dto.TicketResponse;
import com.atl.ticketmgmt.ticket.api.dto.TransitionTicketRequest;
import com.atl.ticketmgmt.ticket.api.dto.UpdateTicketRequest;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import com.atl.ticketmgmt.ticket.service.CommentService;
import com.atl.ticketmgmt.ticket.service.TicketService;
import com.atl.ticketmgmt.ticket.service.TicketStatusTransitionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketStatusTransitionService transitionService;
    private final CommentService commentService;

    public TicketController(
            TicketService ticketService,
            TicketStatusTransitionService transitionService,
            CommentService commentService) {
        this.ticketService = ticketService;
        this.transitionService = transitionService;
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse created = ticketService.create(request);
        return ResponseEntity
                .created(URI.create("/api/tickets/" + created.ticketId()))
                .body(created);
    }

    @GetMapping
    public List<TicketResponse> list(
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) TicketStatus status) {
        return ticketService.list(query, status);
    }

    @GetMapping("/{ticketId}")
    public TicketDetailResponse get(@PathVariable String ticketId) {
        return ticketService.getByTicketId(ticketId);
    }

    @PatchMapping("/{ticketId}")
    public TicketDetailResponse update(
            @PathVariable String ticketId, @Valid @RequestBody UpdateTicketRequest request) {
        return ticketService.update(ticketId, request);
    }

    @PostMapping("/{ticketId}/transitions")
    public TicketDetailResponse transition(
            @PathVariable String ticketId, @Valid @RequestBody TransitionTicketRequest request) {
        return transitionService.transition(ticketId, request.status());
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable String ticketId, @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse created = commentService.addComment(ticketId, request);
        return ResponseEntity
                .created(URI.create("/api/tickets/" + ticketId))
                .body(created);
    }
}
