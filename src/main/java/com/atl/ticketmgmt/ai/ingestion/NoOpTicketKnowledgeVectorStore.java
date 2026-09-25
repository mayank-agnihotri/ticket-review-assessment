package com.atl.ticketmgmt.ai.ingestion;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rag.vector-store", name = "type", havingValue = "noop")
public class NoOpTicketKnowledgeVectorStore implements TicketKnowledgeVectorStore {

    private static final Logger log = LoggerFactory.getLogger(NoOpTicketKnowledgeVectorStore.class);

    @Override
    public void replaceForTicket(String ticketId, List<KnowledgeChunk> chunks) {
        log.debug("Vector store stub: replace {} chunks for ticket {}", chunks.size(), ticketId);
    }
}
