package com.atl.ticketmgmt.ai.ingestion;

import com.atl.ticketmgmt.common.config.AppProperties;
import com.atl.ticketmgmt.ticket.domain.Comment;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.repository.CommentRepository;
import com.atl.ticketmgmt.ticket.repository.TicketRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TicketIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TicketIngestionService.class);

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final KnowledgeDocumentBuilder documentBuilder;
    private final ChunkingService chunkingService;
    private final TicketKnowledgeVectorStore vectorStore;
    private final AppProperties appProperties;

    public TicketIngestionService(
            TicketRepository ticketRepository,
            CommentRepository commentRepository,
            KnowledgeDocumentBuilder documentBuilder,
            ChunkingService chunkingService,
            TicketKnowledgeVectorStore vectorStore,
            AppProperties appProperties) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.documentBuilder = documentBuilder;
        this.chunkingService = chunkingService;
        this.vectorStore = vectorStore;
        this.appProperties = appProperties;
    }

    public void scheduleIngest(String ticketId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            ingestSynchronously(ticketId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ingestSynchronously(ticketId);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ingestSynchronously(String ticketId) {
        ticketRepository.findByTicketId(ticketId).ifPresentOrElse(this::ingestTicket, () -> log.warn(
                "Skipping ingestion for missing ticket {}", ticketId));
    }

    void ingestTicket(Ticket ticket) {
        List<Comment> comments =
                commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId());
        KnowledgeDocument document = documentBuilder.build(ticket, comments);

        if (!document.hasIngestibleContent()) {
            vectorStore.replaceForTicket(ticket.getTicketId(), List.of());
            return;
        }

        int maxChunkChars = appProperties.getRag().getMaxChunkChars();
        List<KnowledgeChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        for (KnowledgeSection section : document.sections()) {
            for (String chunkText : chunkingService.chunkText(section.text(), maxChunkChars)) {
                chunks.add(toChunk(ticket, chunkText, chunkIndex++, section.sourceType()));
            }
        }

        vectorStore.replaceForTicket(ticket.getTicketId(), chunks);
    }

    private static KnowledgeChunk toChunk(
            Ticket ticket, String content, int chunkIndex, ChunkSourceType sourceType) {
        return new KnowledgeChunk(
                content,
                ticket.getTicketId(),
                ticket.getStatus(),
                ticket.getPriority(),
                nullToEmpty(ticket.getAssignee()),
                nullToEmpty(ticket.getCategory()),
                chunkIndex,
                sourceType);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
