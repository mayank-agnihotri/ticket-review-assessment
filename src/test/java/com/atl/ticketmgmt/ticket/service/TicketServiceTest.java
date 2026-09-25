package com.atl.ticketmgmt.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atl.ticketmgmt.ai.ingestion.TicketIngestionTrigger;
import com.atl.ticketmgmt.common.config.AppProperties;
import com.atl.ticketmgmt.common.exception.ResourceNotFoundException;
import com.atl.ticketmgmt.ticket.api.TicketMapper;
import com.atl.ticketmgmt.ticket.api.dto.CreateTicketRequest;
import com.atl.ticketmgmt.ticket.api.dto.UpdateTicketRequest;
import com.atl.ticketmgmt.ticket.domain.Priority;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import com.atl.ticketmgmt.ticket.repository.TicketPublicIdSequenceRepository;
import com.atl.ticketmgmt.ticket.repository.TicketRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketPublicIdSequenceRepository sequenceRepository;

    @Mock
    private TicketDetailLoader ticketDetailLoader;

    @Mock
    private TicketIngestionTrigger ingestionTrigger;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getTickets().setListMax(500);
        ticketService = new TicketService(
                ticketRepository,
                sequenceRepository,
                new TicketMapper(),
                properties,
                ticketDetailLoader,
                ingestionTrigger);
    }

    @Test
    void createAssignsOpenStatusAndPublicId() {
        when(sequenceRepository.nextValue()).thenReturn(1001L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = ticketService.create(new CreateTicketRequest(
                "Title", "Description", Priority.HIGH, "agent@example.com", "payments"));

        assertThat(response.ticketId()).isEqualTo("TKT-1001");
        assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void getByTicketIdThrowsWhenMissing() {
        when(ticketRepository.findByTicketId("TKT-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getByTicketId("TKT-404"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        Ticket ticket = new Ticket("TKT-1001", "Old", "Desc", Priority.LOW, TicketStatus.OPEN);
        when(ticketRepository.findByTicketId("TKT-1001")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketDetailLoader.load(ticket)).thenAnswer(invocation -> {
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
                    List.of());
        });

        var detail = ticketService.update("TKT-1001", new UpdateTicketRequest("New title", null, null, null));

        assertThat(detail.title()).isEqualTo("New title");
        assertThat(detail.description()).isEqualTo("Desc");
        assertThat(detail.status()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void listUsesConfiguredMax() {
        when(ticketRepository.findForList(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ticketService.list(null, null);

        verify(ticketRepository).findForList(eq(null), eq(null), any(Pageable.class));
    }
}
