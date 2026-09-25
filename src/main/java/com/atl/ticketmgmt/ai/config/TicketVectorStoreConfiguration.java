package com.atl.ticketmgmt.ai.config;

import com.atl.ticketmgmt.ai.ingestion.PgVectorTicketKnowledgeVectorStore;
import com.atl.ticketmgmt.ai.ingestion.TicketKnowledgeVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.rag.vector-store", name = "type", havingValue = "pgvector", matchIfMissing = true)
public class TicketVectorStoreConfiguration {

    @Bean
    TicketKnowledgeVectorStore ticketKnowledgeVectorStore(VectorStore vectorStore) {
        return new PgVectorTicketKnowledgeVectorStore(vectorStore);
    }
}
