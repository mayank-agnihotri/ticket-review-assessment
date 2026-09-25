package com.atl.ticketmgmt.ai.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atl.ticketmgmt.common.config.AppProperties;
import com.atl.ticketmgmt.ticket.domain.Comment;
import com.atl.ticketmgmt.ticket.domain.Priority;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import com.atl.ticketmgmt.ticket.repository.CommentRepository;
import com.atl.ticketmgmt.ticket.repository.TicketRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketIngestionServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketKnowledgeVectorStore vectorStore;

    @Captor
    private ArgumentCaptor<List<KnowledgeChunk>> chunksCaptor;

    private TicketIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getRag().setMaxChunkChars(1500);
        ingestionService = new TicketIngestionService(
                ticketRepository,
                commentRepository,
                new KnowledgeDocumentBuilder(),
                new ChunkingService(),
                vectorStore,
                properties);
    }

    @Test
    void ingestBuildsChunksWithMetadata() {
        Ticket ticket = new Ticket("TKT-2001", "Title", "Body text", Priority.CRITICAL, TicketStatus.IN_PROGRESS);
        ticket.setCategory("billing");
        ticket.setAssignee("agent@example.com");
        when(ticketRepository.findByTicketId("TKT-2001")).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicket_IdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        ingestionService.ingestSynchronously("TKT-2001");

        verify(vectorStore).replaceForTicket(eq("TKT-2001"), chunksCaptor.capture());
        List<KnowledgeChunk> chunks = chunksCaptor.getValue();
        assertThat(chunks).isNotEmpty();
        KnowledgeChunk first = chunks.get(0);
        assertThat(first.ticketId()).isEqualTo("TKT-2001");
        assertThat(first.status()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(first.priority()).isEqualTo(Priority.CRITICAL);
        assertThat(first.assignee()).isEqualTo("agent@example.com");
        assertThat(first.category()).isEqualTo("billing");
        assertThat(first.chunkIndex()).isZero();
        assertThat(first.sourceType()).isEqualTo(ChunkSourceType.DESCRIPTION);
    }

    @Test
    void ingestDeletesOnlyWhenNoIngestibleContent() {
        Ticket ticket = new Ticket("TKT-2002", "", "", Priority.LOW, TicketStatus.OPEN);
        when(ticketRepository.findByTicketId("TKT-2002")).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicket_IdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        ingestionService.ingestSynchronously("TKT-2002");

        verify(vectorStore).replaceForTicket("TKT-2002", List.of());
    }

    @Test
    void ingestIncludesCommentChunksWithIncrementedIndex() {
        Ticket ticket = new Ticket("TKT-2003", "T", "D", Priority.LOW, TicketStatus.OPEN);
        Comment comment = new Comment(ticket, "Follow-up note", "system");
        when(ticketRepository.findByTicketId("TKT-2003")).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicket_IdOrderByCreatedAtAsc(any())).thenReturn(List.of(comment));

        ingestionService.ingestSynchronously("TKT-2003");

        verify(vectorStore).replaceForTicket(eq("TKT-2003"), chunksCaptor.capture());
        assertThat(chunksCaptor.getValue()).extracting(KnowledgeChunk::sourceType)
                .contains(ChunkSourceType.DESCRIPTION, ChunkSourceType.COMMENT);
        assertThat(chunksCaptor.getValue()).extracting(KnowledgeChunk::chunkIndex).contains(0, 1);
    }
}
