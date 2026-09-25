package com.atl.ticketmgmt.ticket.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("repository-test")
class TicketSearchFilterApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String titleMatchId;
    private String descriptionMatchId;
    private String commentOnlyId;

    private String titleToken;
    private String descToken;
    private String commentToken;

    @BeforeEach
    void seedTickets() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        titleToken = "UniqueTitleToken" + suffix;
        descToken = "UniqueDescToken" + suffix;
        commentToken = "UniqueCommentOnlyToken" + suffix;

        titleMatchId = createTicket(titleToken, "plain description", "LOW");
        descriptionMatchId = createTicket("Other title", "Contains " + descToken + " here", "LOW");
        commentOnlyId = createTicket("Another title", "plain", "LOW");

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", commentOnlyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + commentToken + " should not match search\"}"))
                .andExpect(status().isCreated());

        transition(titleMatchId, "IN_PROGRESS");
    }

    @Test
    void searchMatchesTitleCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/tickets").param("q", titleToken.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ticketId == '" + titleMatchId + "')]").exists());
    }

    @Test
    void searchMatchesDescription() throws Exception {
        mockMvc.perform(get("/api/tickets").param("q", descToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ticketId == '" + descriptionMatchId + "')]").exists());
    }

    @Test
    void searchMatchesTicketIdSubstring() throws Exception {
        mockMvc.perform(get("/api/tickets").param("q", titleMatchId.substring(4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ticketId == '" + titleMatchId + "')]").exists());
    }

    @Test
    void searchDoesNotMatchCommentBodies() throws Exception {
        mockMvc.perform(get("/api/tickets").param("q", commentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void filterByStatus() throws Exception {
        mockMvc.perform(get("/api/tickets").param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ticketId == '" + titleMatchId + "')]").exists())
                .andExpect(jsonPath("$[?(@.ticketId == '" + descriptionMatchId + "')]").doesNotExist());
    }

    @Test
    void combinedSearchAndStatusFilter() throws Exception {
        mockMvc.perform(get("/api/tickets").param("q", titleToken).param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/tickets").param("q", titleToken).param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticketId").value(titleMatchId));
    }

    private String createTicket(String title, String description, String priority) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                                {"title":"%s","description":"%s","priority":"%s"}
                                """,
                                title, description, priority)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        return created.get("ticketId").asText();
    }

    private void transition(String ticketId, String status) throws Exception {
        mockMvc.perform(post("/api/tickets/{ticketId}/transitions", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }
}
