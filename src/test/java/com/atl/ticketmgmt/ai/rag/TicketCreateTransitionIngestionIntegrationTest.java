package com.atl.ticketmgmt.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atl.ticketmgmt.ai.support.DeterministicPgVectorTestConfiguration;
import com.atl.ticketmgmt.ai.support.RagIntegrationConditions;
import com.atl.ticketmgmt.ai.support.RagTestFixtures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("pgvector-it")
@Import(DeterministicPgVectorTestConfiguration.class)
@EnabledIf("com.atl.ticketmgmt.ai.support.RagIntegrationConditions#isPostgresReady")
class TicketCreateTransitionIngestionIntegrationTest {

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
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanVectors() {
        jdbcTemplate.execute("TRUNCATE TABLE ticket_vector_store");
    }

    @Test
    void createThenTransitionIndexesDescriptionWithoutPatch() throws Exception {
        String descriptionToken = RagTestFixtures.token("create-transition-index");
        String ticketId = createTicket(descriptionToken);

        mockMvc.perform(post("/api/tickets/{ticketId}/transitions", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        String stored = jdbcTemplate.queryForObject(
                "SELECT string_agg(content, E'\\n') FROM ticket_vector_store WHERE metadata->>'ticketId' = ?",
                String.class,
                ticketId);
        assertThat(stored).isNotNull().contains(descriptionToken);
    }

    private String createTicket(String descriptionToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Onboarding","description":"Initial description %s","priority":"LOW"}
                                """
                                .formatted(descriptionToken)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        return created.get("ticketId").asText();
    }
}
