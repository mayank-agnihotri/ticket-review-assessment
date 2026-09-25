package com.atl.ticketmgmt.ai.ingestion;

import com.atl.ticketmgmt.ai.vector.KnowledgeVectorMetadata;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
public class PgVectorTicketKnowledgeVectorStore implements TicketKnowledgeVectorStore {

    private final VectorStore vectorStore;

    public PgVectorTicketKnowledgeVectorStore(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void replaceForTicket(String ticketId, List<KnowledgeChunk> chunks) {
        vectorStore.delete(new FilterExpressionBuilder()
                .eq(KnowledgeVectorMetadata.TICKET_ID, ticketId)
                .build());

        if (chunks.isEmpty()) {
            return;
        }

        List<Document> documents = chunks.stream().map(this::toDocument).toList();
        vectorStore.add(documents);
    }

    private Document toDocument(KnowledgeChunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(KnowledgeVectorMetadata.TICKET_ID, chunk.ticketId());
        metadata.put(KnowledgeVectorMetadata.STATUS, chunk.status().name());
        metadata.put(KnowledgeVectorMetadata.PRIORITY, chunk.priority().name());
        metadata.put(KnowledgeVectorMetadata.ASSIGNEE, chunk.assignee());
        metadata.put(KnowledgeVectorMetadata.CATEGORY, chunk.category());
        metadata.put(KnowledgeVectorMetadata.CHUNK_INDEX, chunk.chunkIndex());
        metadata.put(KnowledgeVectorMetadata.SOURCE_TYPE, chunk.sourceType().name());
        return new Document(UUID.randomUUID().toString(), chunk.content(), metadata);
    }
}
