package com.atl.ticketmgmt.ai.vector;

import static org.assertj.core.api.Assertions.assertThat;

import com.atl.ticketmgmt.ai.ingestion.ChunkSourceType;
import com.atl.ticketmgmt.ai.ingestion.KnowledgeChunk;
import com.atl.ticketmgmt.ai.ingestion.TicketIngestionService;
import com.atl.ticketmgmt.ai.ingestion.TicketKnowledgeVectorStore;
import com.atl.ticketmgmt.ai.rag.TicketKnowledgeRetrievalService;
import com.atl.ticketmgmt.ai.support.DeterministicPgVectorTestConfiguration;
import com.atl.ticketmgmt.ticket.domain.Priority;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import com.atl.ticketmgmt.ticket.repository.TicketPublicIdSequenceRepository;
import com.atl.ticketmgmt.ticket.repository.TicketRepository;
import com.atl.ticketmgmt.ticket.support.TicketPublicIdFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("pgvector-it")
@Import(DeterministicPgVectorTestConfiguration.class)
@EnabledIf("com.atl.ticketmgmt.ai.support.RagIntegrationConditions#isPostgresReady")
class PgVectorTicketKnowledgeVectorStoreIntegrationTest {

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> System.getenv().getOrDefault(
                        "PGVECTOR_IT_JDBC_URL", "jdbc:postgresql://localhost:5432/ticketmgmt"));
        registry.add(
                "spring.datasource.username",
                () -> System.getenv().getOrDefault("PGVECTOR_IT_DB_USER", "ticket"));
        registry.add(
                "spring.datasource.password",
                () -> System.getenv().getOrDefault("PGVECTOR_IT_DB_PASSWORD", "ticket"));
    }

    @Autowired
    private TicketKnowledgeVectorStore vectorStore;

    @Autowired
    private TicketKnowledgeRetrievalService retrievalService;

    @Autowired
    private TicketIngestionService ingestionService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketPublicIdSequenceRepository sequenceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanVectorTable() {
        jdbcTemplate.execute("TRUNCATE TABLE ticket_vector_store");
    }

    @Test
    void replaceForTicketDeletesPreviousChunksForSameTicketId() {
        String ticketId = "TKT-9001";
        KnowledgeChunk first = sampleChunk(ticketId, "First chunk body", 0);
        KnowledgeChunk second = sampleChunk(ticketId, "Second chunk body", 0);

        vectorStore.replaceForTicket(ticketId, List.of(first));
        assertThat(chunkCountForTicket(ticketId)).isEqualTo(1);
        assertThat(chunkContentForTicket(ticketId)).isEqualTo("First chunk body");

        vectorStore.replaceForTicket(ticketId, List.of(second));
        assertThat(chunkCountForTicket(ticketId)).isEqualTo(1);
        assertThat(chunkContentForTicket(ticketId)).isEqualTo("Second chunk body");
    }

    @Test
    void ingestionPipelineStoresChunksWithRequiredMetadataOnPostgres() {
        String publicId = TicketPublicIdFormatter.format(sequenceRepository.nextValue());
        Ticket ticket = new Ticket(publicId, "Billing issue", "Refund requested", Priority.HIGH, TicketStatus.OPEN);
        ticket.setCategory("billing");
        ticket.setAssignee("agent@example.com");
        ticketRepository.saveAndFlush(ticket);

        ingestionService.ingestSynchronously(publicId);

        List<Document> hits = hitsForTicket("Title: Billing issue\nDescription: Refund requested", publicId);
        assertThat(hits).isNotEmpty();
        Document hit = hits.get(0);
        assertThat(hit.getMetadata().get(KnowledgeVectorMetadata.TICKET_ID)).isEqualTo(publicId);
        assertThat(hit.getMetadata().get(KnowledgeVectorMetadata.STATUS)).isEqualTo("OPEN");
        assertThat(hit.getMetadata().get(KnowledgeVectorMetadata.PRIORITY)).isEqualTo("HIGH");
        assertThat(hit.getMetadata().get(KnowledgeVectorMetadata.ASSIGNEE)).isEqualTo("agent@example.com");
        assertThat(hit.getMetadata().get(KnowledgeVectorMetadata.CATEGORY)).isEqualTo("billing");
        assertThat(hit.getMetadata()).containsKeys(KnowledgeVectorMetadata.CHUNK_INDEX, KnowledgeVectorMetadata.SOURCE_TYPE);
    }

    @Test
    void reIngestAfterTicketUpdateRefreshesRetrievedContent() {
        String publicId = TicketPublicIdFormatter.format(sequenceRepository.nextValue());
        Ticket ticket = new Ticket(publicId, "Title", "Original description", Priority.LOW, TicketStatus.OPEN);
        ticketRepository.saveAndFlush(ticket);

        ingestionService.ingestSynchronously(publicId);
        assertThat(hitsForTicket("Title: Title\nDescription: Original description", publicId)).isNotEmpty();
        assertThat(hitsForTicket("Title: Title\nDescription: Updated description for vectors", publicId))
                .isEmpty();

        ticket.setDescription("Updated description for vectors");
        ticketRepository.saveAndFlush(ticket);
        ingestionService.ingestSynchronously(publicId);

        assertThat(hitsForTicket("Title: Title\nDescription: Original description", publicId)).isEmpty();
        assertThat(hitsForTicket("Title: Title\nDescription: Updated description for vectors", publicId))
                .isNotEmpty();
    }

    private List<Document> hitsForTicket(String query, String ticketId) {
        return retrievalService.search(query).stream()
                .filter(document -> ticketId.equals(document.getMetadata().get(KnowledgeVectorMetadata.TICKET_ID)))
                .toList();
    }

    private int chunkCountForTicket(String ticketId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_vector_store WHERE metadata->>'ticketId' = ?",
                Integer.class,
                ticketId);
        return count == null ? 0 : count;
    }

    private String chunkContentForTicket(String ticketId) {
        return jdbcTemplate.queryForObject(
                "SELECT content FROM ticket_vector_store WHERE metadata->>'ticketId' = ? LIMIT 1",
                String.class,
                ticketId);
    }

    private static KnowledgeChunk sampleChunk(String ticketId, String content, int index) {
        return new KnowledgeChunk(
                content,
                ticketId,
                TicketStatus.OPEN,
                Priority.MEDIUM,
                "agent@example.com",
                "payments",
                index,
                ChunkSourceType.DESCRIPTION);
    }

}
