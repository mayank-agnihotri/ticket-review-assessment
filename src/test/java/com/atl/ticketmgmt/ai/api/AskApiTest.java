package com.atl.ticketmgmt.ai.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atl.ticketmgmt.ai.api.dto.AiAskResponse;
import com.atl.ticketmgmt.ai.api.dto.AiAskRetrievalInfo;
import com.atl.ticketmgmt.ai.rag.RagService;
import com.atl.ticketmgmt.common.api.GlobalExceptionHandler;
import com.atl.ticketmgmt.common.exception.AiServiceUnavailableException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AskApiTest {

    @Mock
    private RagService ragService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AiAskController(ragService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void validationFailureReturns400() throws Exception {
        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void returnsGroundedResponseShape() throws Exception {
        when(ragService.ask(anyString()))
                .thenReturn(new AiAskResponse(
                        "Prior tickets mention card declines.",
                        List.of("TKT-1001"),
                        true,
                        false,
                        new AiAskRetrievalInfo(5, 0.65, 2)));

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What caused payment failures?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grounded").value(true))
                .andExpect(jsonPath("$.noMatch").value(false))
                .andExpect(jsonPath("$.citedTicketIds[0]").value("TKT-1001"))
                .andExpect(jsonPath("$.retrieval.topK").value(5))
                .andExpect(jsonPath("$.retrieval.similarityThreshold").value(0.65))
                .andExpect(jsonPath("$.retrieval.chunksUsed").value(2));
    }

    @Test
    void returnsNoMatchResponseShape() throws Exception {
        when(ragService.ask(anyString()))
                .thenReturn(new AiAskResponse(
                        RagService.NO_MATCH_ANSWER,
                        List.of(),
                        false,
                        true,
                        new AiAskRetrievalInfo(5, 0.65, 0)));

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Unknown issue?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(RagService.NO_MATCH_ANSWER))
                .andExpect(jsonPath("$.noMatch").value(true))
                .andExpect(jsonPath("$.grounded").value(false))
                .andExpect(jsonPath("$.citedTicketIds").isEmpty())
                .andExpect(jsonPath("$.retrieval.chunksUsed").value(0));
    }

    @Test
    void aiFailureReturns503() throws Exception {
        when(ragService.ask(anyString())).thenThrow(new AiServiceUnavailableException("AI retrieval or generation failed"));

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What happened?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_SERVICE_UNAVAILABLE"));
    }
}
