package com.atl.ticketmgmt.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atl.ticketmgmt.ai.ingestion.TicketIngestionTrigger;
import com.atl.ticketmgmt.common.exception.ResourceNotFoundException;
import com.atl.ticketmgmt.ticket.api.TicketMapper;
import com.atl.ticketmgmt.ticket.api.dto.CreateCommentRequest;
import com.atl.ticketmgmt.ticket.domain.Comment;
import com.atl.ticketmgmt.ticket.domain.Priority;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import com.atl.ticketmgmt.ticket.repository.CommentRepository;
import com.atl.ticketmgmt.ticket.repository.TicketRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketIngestionTrigger ingestionTrigger;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService =
                new CommentService(ticketRepository, commentRepository, new TicketMapper(), ingestionTrigger);
    }

    @Test
    void addCommentPersistsWithSystemAuthor() {
        Ticket ticket = new Ticket("TKT-1001", "T", "D", Priority.LOW, TicketStatus.OPEN);
        when(ticketRepository.findByTicketId("TKT-1001")).thenReturn(Optional.of(ticket));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = commentService.addComment("TKT-1001", new CreateCommentRequest("Hello"));

        assertThat(response.body()).isEqualTo("Hello");
        assertThat(response.author()).isEqualTo(CommentService.DEFAULT_AUTHOR);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addCommentThrowsWhenTicketMissing() {
        when(ticketRepository.findByTicketId("TKT-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment("TKT-404", new CreateCommentRequest("Hi")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
