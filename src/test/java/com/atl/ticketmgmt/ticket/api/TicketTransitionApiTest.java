package com.atl.ticketmgmt.ticket.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class TicketTransitionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullHappyPathTransitions() throws Exception {
        String ticketId = createTicket();

        transition(ticketId, "IN_PROGRESS").andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        transition(ticketId, "RESOLVED").andExpect(jsonPath("$.status").value("RESOLVED"));
        transition(ticketId, "CLOSED").andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void openCanBeCancelled() throws Exception {
        String ticketId = createTicket();
        transition(ticketId, "CANCELLED").andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void inProgressCanBeCancelled() throws Exception {
        String ticketId = createTicket();
        transition(ticketId, "IN_PROGRESS");
        transition(ticketId, "CANCELLED").andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void rejectsClosedToOpen() throws Exception {
        String ticketId = createTicket();
        transition(ticketId, "IN_PROGRESS");
        transition(ticketId, "RESOLVED");
        transition(ticketId, "CLOSED");

        mockMvc.perform(post("/api/tickets/{ticketId}/transitions", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void rejectsResolvedToOpen() throws Exception {
        String ticketId = createTicket();
        transition(ticketId, "IN_PROGRESS");
        transition(ticketId, "RESOLVED");

        mockMvc.perform(post("/api/tickets/{ticketId}/transitions", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void rejectsCancelledToOpen() throws Exception {
        String ticketId = createTicket();
        transition(ticketId, "CANCELLED");

        mockMvc.perform(post("/api/tickets/{ticketId}/transitions", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void rejectsOpenToResolved() throws Exception {
        String ticketId = createTicket();

        mockMvc.perform(post("/api/tickets/{ticketId}/transitions", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void transitionUnknownTicketReturns404() throws Exception {
        mockMvc.perform(post("/api/tickets/TKT-999999/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private String createTicket() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"T","description":"D","priority":"LOW"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        return created.get("ticketId").asText();
    }

    private org.springframework.test.web.servlet.ResultActions transition(String ticketId, String status)
            throws Exception {
        return mockMvc.perform(post("/api/tickets/{ticketId}/transitions", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}"));
    }
}
