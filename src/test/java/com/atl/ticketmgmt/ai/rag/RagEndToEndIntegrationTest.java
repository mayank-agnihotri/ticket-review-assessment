package com.atl.ticketmgmt.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atl.ticketmgmt.ai.api.dto.AiAskResponse;
import com.atl.ticketmgmt.ai.ingestion.TicketIngestionService;
import com.atl.ticketmgmt.ai.support.RagIntegrationConditions;
import com.atl.ticketmgmt.ai.support.RagTestFixtures;
import com.atl.ticketmgmt.ai.vector.KnowledgeVectorMetadata;
import com.atl.ticketmgmt.common.config.AppProperties;
import com.atl.ticketmgmt.ticket.api.dto.CreateCommentRequest;
import com.atl.ticketmgmt.ticket.domain.Priority;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import com.atl.ticketmgmt.ticket.repository.TicketPublicIdSequenceRepository;
import com.atl.ticketmgmt.ticket.repository.TicketRepository;
import com.atl.ticketmgmt.ticket.service.CommentService;
import com.atl.ticketmgmt.ticket.service.TicketStatusTransitionService;
import com.atl.ticketmgmt.ticket.support.TicketPublicIdFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("rag-e2e-it")
@Tag("ollama-e2e")
@EnabledIf("com.atl.ticketmgmt.ai.support.RagIntegrationConditions#isEnvironmentReady")
class RagEndToEndIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketPublicIdSequenceRepository sequenceRepository;

    @Autowired
    private TicketIngestionService ingestionService;

    @Autowired
    private TicketKnowledgeRetrievalService retrievalService;

    @Autowired
    private RagService ragService;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CommentService commentService;

    @Autowired
    private TicketStatusTransitionService transitionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void logReadiness() {
        if (!RagIntegrationConditions.isEnvironmentReady()) {
            System.out.println("RAG E2E environment not ready:\n" + RagIntegrationConditions.readinessReport());
        }
    }

    @BeforeEach
    void cleanVectors() {
        jdbcTemplate.execute("TRUNCATE TABLE ticket_vector_store");
        appProperties.getRag().setTopK(2);
        appProperties.getRag().setSimilarityThreshold(0.45);
    }

    @Test
    void ticketIngestionEmbedsIntoPgVector() {
        String ticketId = createTicket("Checkout", RagTestFixtures.token("embed-pipeline"), Priority.HIGH, TicketStatus.OPEN);
        ingestionService.ingestSynchronously(ticketId);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_vector_store WHERE metadata->>'ticketId' = ?",
                Integer.class,
                ticketId);
        assertThat(count).isGreaterThan(0);
        Integer embedded = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_vector_store WHERE metadata->>'ticketId' = ? AND embedding IS NOT NULL",
                Integer.class,
                ticketId);
        assertThat(embedded).isEqualTo(count);
    }

    @Test
    void retrievalHonorsTopK() {
        String cluster = RagTestFixtures.token("cluster");
        saveTicket("Payment A", "Shared payment issue " + cluster, Priority.LOW, TicketStatus.OPEN);
        saveTicket("Payment B", "Shared payment issue " + cluster, Priority.LOW, TicketStatus.OPEN);
        saveTicket("Payment C", "Shared payment issue " + cluster, Priority.LOW, TicketStatus.OPEN);

        List<Document> hits = retrievalService.search("What caused the shared payment issue " + cluster + "?");

        assertThat(hits).isNotEmpty();
        assertThat(hits.size()).isLessThanOrEqualTo(appProperties.getRag().getTopK());
    }

    @Test
    void retrievalHonorsSimilarityThreshold() {
        String token = RagTestFixtures.token("threshold");
        String ticketId = saveTicket("Threshold", "Customer saw error " + token, Priority.MEDIUM, TicketStatus.OPEN);
        ingestionService.ingestSynchronously(ticketId);

        appProperties.getRag().setSimilarityThreshold(0.99);
        assertThat(hitsForTicket("Unrelated database latency question", ticketId)).isEmpty();

        appProperties.getRag().setSimilarityThreshold(0.45);
        assertThat(hitsForTicket("What error did the customer see " + token + "?", ticketId)).isNotEmpty();
    }

    @Test
    void askEndpointSucceedsWhenRetrievalFindsTicketContext() throws Exception {
        String token = RagTestFixtures.token("ask-retrieval");
        String ticketId = saveTicket(
                "Card decline",
                "Payment gateway declined card with code " + token,
                Priority.HIGH,
                TicketStatus.OPEN);
        ingestionService.ingestSynchronously(ticketId);

        String question = "Payment gateway declined card with code " + token;
        assertThat(hitsForTicket(question, ticketId)).isNotEmpty();

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + question + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retrieval.chunksUsed").isNumber())
                .andExpect(jsonPath("$.citedTicketIds").isArray());
    }

    @Test
    void askReturnsNoMatchWhenNoRelevantChunks() {
        AiAskResponse response = ragService.ask("What caused " + RagTestFixtures.token("nomatch-phantom") + "?");

        assertThat(response.noMatch()).isTrue();
        assertThat(response.grounded()).isFalse();
        assertThat(response.answer()).isEqualTo(RagService.NO_MATCH_ANSWER);
        assertThat(response.citedTicketIds()).isEmpty();
        assertThat(response.retrieval().chunksUsed()).isZero();
    }

    @Test
    void updatedTicketRefreshesSearchableContent() throws Exception {
        String oldToken = RagTestFixtures.token("before-update");
        String newToken = RagTestFixtures.token("after-update");
        String ticketId = saveTicket("Mutable", "Initial state " + oldToken, Priority.LOW, TicketStatus.OPEN);
        ingestionService.ingestSynchronously(ticketId);

        assertThat(vectorContentForTicket(ticketId)).contains(oldToken);

        mockMvc.perform(patch("/api/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Updated state " + newToken + "\"}"))
                .andExpect(status().isOk());

        assertThat(vectorContentForTicket(ticketId)).contains(newToken);
        assertThat(vectorContentForTicket(ticketId)).doesNotContain(oldToken);
        assertThat(hitsForTicket("Updated state " + newToken, ticketId)).isNotEmpty();
    }

    @Test
    void closedTicketUpdatesVectorMetadata() {
        String token = RagTestFixtures.token("closed-meta");
        String ticketId = saveTicket("Lifecycle", "Issue details " + token, Priority.LOW, TicketStatus.OPEN);
        ingestionService.ingestSynchronously(ticketId);

        transitionService.transition(ticketId, TicketStatus.IN_PROGRESS);
        transitionService.transition(ticketId, TicketStatus.RESOLVED);
        transitionService.transition(ticketId, TicketStatus.CLOSED);

        String status = jdbcTemplate.queryForObject(
                "SELECT metadata->>'status' FROM ticket_vector_store WHERE metadata->>'ticketId' = ? LIMIT 1",
                String.class,
                ticketId);
        assertThat(status).isEqualTo("CLOSED");
    }

    @Test
    void commentsAndResolutionNotesAreIndexedForRetrieval() {
        String commentToken = RagTestFixtures.token("comment-body");
        String resolutionToken = RagTestFixtures.token("resolution-note");
        String ticketId = saveTicket("Support", "Base description only", Priority.MEDIUM, TicketStatus.OPEN);

        commentService.addComment(ticketId, new CreateCommentRequest("Customer note " + commentToken));

        Ticket ticket = ticketRepository.findByTicketId(ticketId).orElseThrow();
        ticket.setResolutionNotes("Resolved by refund " + resolutionToken);
        ticketRepository.saveAndFlush(ticket);
        ingestionService.ingestSynchronously(ticketId);

        String stored = vectorContentForTicket(ticketId);
        assertThat(stored).contains(commentToken);
        assertThat(stored).contains(resolutionToken);

        Set<String> retrievedText = retrievalService
                .search("What did the customer note about " + commentToken + " and refund " + resolutionToken + "?")
                .stream()
                .map(Document::getText)
                .collect(Collectors.toSet());
        assertThat(retrievedText.stream().anyMatch(text -> text.contains(commentToken))).isTrue();
        assertThat(retrievedText.stream().anyMatch(text -> text.contains(resolutionToken))).isTrue();
    }

    private String saveTicket(String title, String description, Priority priority, TicketStatus status) {
        String ticketId = createTicket(title, description, priority, status);
        ingestionService.ingestSynchronously(ticketId);
        return ticketId;
    }

    private String createTicket(String title, String description, Priority priority, TicketStatus status) {
        String publicId = TicketPublicIdFormatter.format(sequenceRepository.nextValue());
        Ticket ticket = new Ticket(publicId, title, description, priority, status);
        ticketRepository.saveAndFlush(ticket);
        return publicId;
    }

    private List<Document> hitsForTicket(String query, String ticketId) {
        return retrievalService.search(query).stream()
                .filter(document -> ticketId.equals(document.getMetadata().get(KnowledgeVectorMetadata.TICKET_ID)))
                .toList();
    }

    private String vectorContentForTicket(String ticketId) {
        List<String> contents = jdbcTemplate.query(
                "SELECT content FROM ticket_vector_store WHERE metadata->>'ticketId' = ?",
                (rs, rowNum) -> rs.getString("content"),
                ticketId);
        return String.join("\n", contents);
    }
}
