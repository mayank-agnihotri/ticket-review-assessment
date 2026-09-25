package com.atl.ticketmgmt.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atl.ticketmgmt.ai.ingestion.TicketIngestionTrigger;
import com.atl.ticketmgmt.common.exception.InvalidStatusTransitionException;
import com.atl.ticketmgmt.common.exception.ResourceNotFoundException;
import com.atl.ticketmgmt.ticket.api.TicketMapper;
import com.atl.ticketmgmt.ticket.domain.Priority;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import com.atl.ticketmgmt.ticket.repository.TicketRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketStatusTransitionServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketDetailLoader ticketDetailLoader;

    @Mock
    private TicketIngestionTrigger ingestionTrigger;

    private TicketStatusTransitionService transitionService;

    @BeforeEach
    void setUp() {
        transitionService =
                new TicketStatusTransitionService(ticketRepository, ticketDetailLoader, ingestionTrigger);
    }

    @Test
    void transitionUpdatesStatusWhenAllowed() {
        Ticket ticket = new Ticket("TKT-1001", "T", "D", Priority.LOW, TicketStatus.OPEN);
        when(ticketRepository.findByTicketId("TKT-1001")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketDetailLoader.load(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            return new com.atl.ticketmgmt.ticket.api.dto.TicketDetailResponse(
                    saved.getTicketId(),
                    saved.getTitle(),
                    saved.getDescription(),
                    saved.getPriority(),
                    saved.getAssignee(),
                    saved.getCategory(),
                    saved.getStatus(),
                    saved.getResolutionNotes(),
                    saved.getCreatedAt(),
                    saved.getUpdatedAt(),
                    java.util.List.of());
        });

        var response = transitionService.transition("TKT-1001", TicketStatus.IN_PROGRESS);

        assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(ticketRepository).save(ticket);
    }

    @Test
    void transitionThrowsWhenTicketMissing() {
        when(ticketRepository.findByTicketId("TKT-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transitionService.transition("TKT-404", TicketStatus.IN_PROGRESS))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void transitionThrowsWhenNotAllowed() {
        Ticket ticket = new Ticket("TKT-1001", "T", "D", Priority.LOW, TicketStatus.CLOSED);
        when(ticketRepository.findByTicketId("TKT-1001")).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> transitionService.transition("TKT-1001", TicketStatus.OPEN))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
