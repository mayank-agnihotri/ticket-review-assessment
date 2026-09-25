package com.atl.ticketmgmt.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.atl.ticketmgmt.ai.ingestion.TicketIngestionService;
import com.atl.ticketmgmt.ai.support.DeterministicPgVectorTestConfiguration;
import com.atl.ticketmgmt.ai.support.RagIntegrationConditions;
import com.atl.ticketmgmt.ai.support.RagTestFixtures;
import com.atl.ticketmgmt.ai.vector.KnowledgeVectorMetadata;
import com.atl.ticketmgmt.common.config.AppProperties;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Assessment-style retrieval recall (R1–R5) using deterministic embeddings — no Ollama.
 */
@SpringBootTest
@ActiveProfiles("pgvector-it")
@Import(DeterministicPgVectorTestConfiguration.class)
@EnabledIf("com.atl.ticketmgmt.ai.support.RagIntegrationConditions#isPostgresReady")
class RagRetrievalEvaluationIntegrationTest {

    private final String r1Token = RagTestFixtures.token("eval-r1");
    private final String r3Token = RagTestFixtures.token("eval-r3");
    private final String r4Token = RagTestFixtures.token("eval-r4");
    private final String r5Token = RagTestFixtures.token("eval-r5");

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
    private TicketRepository ticketRepository;

    @Autowired
    private TicketPublicIdSequenceRepository sequenceRepository;

    @Autowired
    private TicketIngestionService ingestionService;

    @Autowired
    private TicketKnowledgeRetrievalService retrievalService;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String paymentTicketId;
    private String paymentDescription;
    private String resolutionTicketId;
    private String shipmentTicketId;
    private String resolvedTicketId;
    private String highPaymentTicketId;
    private String highPaymentDescription;

    @BeforeEach
    void seedEvaluationFixtures() {
        jdbcTemplate.execute("TRUNCATE TABLE ticket_vector_store");
        appProperties.getRag().setTopK(10);
        appProperties.getRag().setSimilarityThreshold(0.0);

        paymentDescription = "Customer payment failures at checkout " + r1Token
                + ": card declined and payment gateway timeout during payment processing.";
        paymentTicketId = saveAndIngest(
                "Payment checkout",
                paymentDescription,
                Priority.MEDIUM,
                TicketStatus.OPEN,
                "payments",
                null,
                null);
        saveAndIngest(
                "Unrelated login",
                "Password reset email not received.",
                Priority.LOW,
                TicketStatus.OPEN,
                "auth",
                null,
                null);

        resolutionTicketId = saveAndIngest(
                "Refund case",
                "Order refund requested after payment failure.",
                Priority.MEDIUM,
                TicketStatus.RESOLVED,
                "payments",
                null,
                "Issued full refund to customer card.");
        saveAndIngest(
                "Other refund",
                "Unrelated billing adjustment.",
                Priority.LOW,
                TicketStatus.OPEN,
                "billing",
                null,
                null);

        shipmentTicketId = saveAndIngest(
                "Tracking broken",
                "Shipment tracking issues " + r3Token
                        + ": carrier lost scan events and shipment tracking page shows stale status.",
                Priority.MEDIUM,
                TicketStatus.IN_PROGRESS,
                "logistics",
                null,
                null);
        saveAndIngest(
                "Billing question",
                "Invoice line items unclear.",
                Priority.LOW,
                TicketStatus.OPEN,
                "billing",
                null,
                null);

        resolvedTicketId = saveAndIngest(
                "Warehouse delay",
                "Resolved shipment tracking delay " + r4Token
                        + " at warehouse; shipment tracking updated after carrier rescan.",
                Priority.MEDIUM,
                TicketStatus.RESOLVED,
                "logistics",
                null,
                null);
        saveAndIngest(
                "Open shipment",
                "Shipment tracking issues still open for investigation.",
                Priority.LOW,
                TicketStatus.OPEN,
                "logistics",
                null,
                null);

        highPaymentDescription = "High-priority payment processing failures " + r5Token
                + ": duplicate charge during payment settlement.";
        highPaymentTicketId = saveAndIngest(
                "Duplicate charge",
                highPaymentDescription,
                Priority.HIGH,
                TicketStatus.OPEN,
                "payments",
                null,
                null);
        saveAndIngest(
                "Low payment noise",
                "Minor payment receipt formatting question.",
                Priority.LOW,
                TicketStatus.OPEN,
                "payments",
                null,
                null);
    }

    @Test
    void r1_paymentFailuresRecallPaymentCategoryTicket() {
        String question = "Have we seen payment failures before?\n"
                + descriptionBlock("Payment checkout", "payments", paymentDescription);
        List<Document> hits = hitsForTicket(question, paymentTicketId);

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).getMetadata().get(KnowledgeVectorMetadata.CATEGORY)).isEqualTo("payments");
    }

    @Test
    void r2_resolutionQuestionRecallsTargetTicket() {
        String resolutionChunk = chunkContentContaining(resolutionTicketId, "Resolution:");
        String question = "What was the resolution for ticket " + resolutionTicketId + "?\n" + resolutionChunk;

        assertThat(hitsForTicket(question, resolutionTicketId)).isNotEmpty();
    }

    @Test
    void r3_shipmentTrackingIssuesRecallShipmentTicket() {
        String question = "What are the common causes of shipment tracking issues? " + r3Token;

        assertThat(hitsForTicket(question, shipmentTicketId)).isNotEmpty();
    }

    @Test
    void r4_resolvedTicketsRecallResolvedStatusMetadata() {
        String question = "Show me similar resolved tickets. " + r4Token;
        List<Document> hits = hitsForTicket(question, resolvedTicketId);

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).getMetadata().get(KnowledgeVectorMetadata.STATUS)).isEqualTo("RESOLVED");
    }

    @Test
    void r5_highPriorityPaymentRecallHighPriorityPaymentTicket() {
        String storedChunk = chunkContentContaining(highPaymentTicketId, r5Token);
        String question = "Which high-priority tickets are related to payment? " + r5Token + "\n" + storedChunk;
        List<Document> hits = hitsForTicket(question, highPaymentTicketId);

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).getMetadata().get(KnowledgeVectorMetadata.PRIORITY)).isEqualTo("HIGH");
    }

    private List<Document> hitsForTicket(String query, String ticketId) {
        return retrievalService.search(query).stream()
                .filter(document -> ticketId.equals(document.getMetadata().get(KnowledgeVectorMetadata.TICKET_ID)))
                .limit(appProperties.getRag().getTopK())
                .toList();
    }

    private static String descriptionBlock(String title, String category, String description) {
        return "Title: " + title + "\nCategory: " + category + "\nDescription: " + description;
    }

    private String chunkContentContaining(String ticketId, String textFragment) {
        List<String> contents = jdbcTemplate.query(
                "SELECT content FROM ticket_vector_store WHERE metadata->>'ticketId' = ?",
                (rs, rowNum) -> rs.getString("content"),
                ticketId);
        return contents.stream()
                .filter(content -> content.contains(textFragment))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No chunk for " + ticketId + " containing " + textFragment));
    }

    private String saveAndIngest(
            String title,
            String description,
            Priority priority,
            TicketStatus status,
            String category,
            String assignee,
            String resolutionNotes) {
        String publicId = TicketPublicIdFormatter.format(sequenceRepository.nextValue());
        Ticket ticket = new Ticket(publicId, title, description, priority, status);
        ticket.setCategory(category);
        if (assignee != null) {
            ticket.setAssignee(assignee);
        }
        if (resolutionNotes != null) {
            ticket.setResolutionNotes(resolutionNotes);
        }
        ticketRepository.saveAndFlush(ticket);
        ingestionService.ingestSynchronously(publicId);
        return publicId;
    }
}
