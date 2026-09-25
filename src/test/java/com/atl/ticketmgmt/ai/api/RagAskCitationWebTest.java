package com.atl.ticketmgmt.ai.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atl.ticketmgmt.ai.rag.RagService;
import com.atl.ticketmgmt.ai.rag.TicketKnowledgeRetrievalService;
import com.atl.ticketmgmt.ai.vector.KnowledgeVectorMetadata;
import com.atl.ticketmgmt.common.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * HTTP-level check that {@code citedTicketIds} ⊆ ticket IDs from retrieved chunks.
 */
@ExtendWith(MockitoExtension.class)
class RagAskCitationWebTest {

    @Mock
    private TicketKnowledgeRetrievalService retrievalService;

    @Mock
    private ChatModel chatModel;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        AppProperties properties = new AppProperties();
        properties.getRag().setTopK(5);
        properties.getRag().setSimilarityThreshold(0.65);
        RagService ragService = new RagService(
                retrievalService,
                chatModel,
                properties,
                new ClassPathResource("prompts/rag-ask-system.txt"));

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AiAskController(ragService))
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void citedTicketIdsAreSubsetOfRetrievedTicketIds() throws Exception {
        List<Document> chunks = List.of(
                new Document(
                        "id-1",
                        "Payment gateway timeout",
                        Map.of(KnowledgeVectorMetadata.TICKET_ID, "TKT-1001")),
                new Document(
                        "id-2",
                        "Card declined retry",
                        Map.of(KnowledgeVectorMetadata.TICKET_ID, "TKT-1004")),
                new Document(
                        "id-3",
                        "Duplicate note",
                        Map.of(KnowledgeVectorMetadata.TICKET_ID, "TKT-1001")));
        when(retrievalService.search("payment failures")).thenReturn(chunks);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(
                        List.of(new Generation(new AssistantMessage("Prior tickets TKT-1001 mention timeouts.")))));

        MvcResult result = mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"payment failures\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Set<String> retrievedIds = new HashSet<>();
        for (Document chunk : chunks) {
            retrievedIds.add(chunk.getMetadata().get(KnowledgeVectorMetadata.TICKET_ID).toString());
        }
        Set<String> cited = new HashSet<>();
        body.get("citedTicketIds").forEach(node -> cited.add(node.asText()));

        assertThat(cited).isNotEmpty();
        assertThat(retrievedIds).containsAll(cited);
    }
}
