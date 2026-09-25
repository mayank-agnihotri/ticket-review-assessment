package com.atl.ticketmgmt.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atl.ticketmgmt.ai.api.dto.AiAskResponse;
import com.atl.ticketmgmt.ai.vector.KnowledgeVectorMetadata;
import com.atl.ticketmgmt.common.config.AppProperties;
import com.atl.ticketmgmt.common.exception.AiServiceUnavailableException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private TicketKnowledgeRetrievalService retrievalService;

    @Mock
    private ChatModel chatModel;

    private RagService ragService;

    @BeforeEach
    void setUp() throws IOException {
        AppProperties properties = new AppProperties();
        properties.getRag().setTopK(5);
        properties.getRag().setSimilarityThreshold(0.65);
        ragService = new RagService(
                retrievalService,
                chatModel,
                properties,
                new ClassPathResource("prompts/rag-ask-system.txt"));
    }

    @Test
    void returnsNoMatchWithoutCallingLlmWhenRetrievalIsEmpty() {
        when(retrievalService.search("orphan question")).thenReturn(List.of());

        AiAskResponse response = ragService.ask("orphan question");

        assertThat(response.noMatch()).isTrue();
        assertThat(response.grounded()).isFalse();
        assertThat(response.answer()).isEqualTo(RagService.NO_MATCH_ANSWER);
        assertThat(response.citedTicketIds()).isEmpty();
        assertThat(response.retrieval().chunksUsed()).isZero();
        assertThat(response.retrieval().topK()).isEqualTo(5);
        assertThat(response.retrieval().similarityThreshold()).isEqualTo(0.65);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void returnsGroundedAnswerWithCitationsFromRetrievedChunks() {
        List<Document> chunks = List.of(
                new Document(
                        "id-1",
                        "Card declined at checkout",
                        Map.of(KnowledgeVectorMetadata.TICKET_ID, "TKT-1001")),
                new Document(
                        "id-2",
                        "Retry succeeded",
                        Map.of(KnowledgeVectorMetadata.TICKET_ID, "TKT-1004")),
                new Document(
                        "id-3",
                        "Duplicate ticket note",
                        Map.of(KnowledgeVectorMetadata.TICKET_ID, "TKT-1001")));
        when(retrievalService.search("payment failures")).thenReturn(chunks);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Prior tickets show card declines.")))));

        AiAskResponse response = ragService.ask("payment failures");

        assertThat(response.noMatch()).isFalse();
        assertThat(response.grounded()).isTrue();
        assertThat(response.answer()).contains("card declines");
        assertThat(response.citedTicketIds()).containsExactly("TKT-1001", "TKT-1004");
        assertThat(response.retrieval().chunksUsed()).isEqualTo(3);
        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    void passesOnlyRetrievedContextToLlm() {
        Document chunk = new Document(
                "id-1",
                "Unique excerpt about gateway timeout",
                Map.of(KnowledgeVectorMetadata.TICKET_ID, "TKT-2002"));
        when(retrievalService.search("timeouts")).thenReturn(List.of(chunk));
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("TKT-2002 had gateway timeouts.")))));

        ragService.ask("timeouts");

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        String promptText = promptCaptor.getValue().getInstructions().stream()
                .map(message -> message.getText())
                .reduce("", String::concat);
        assertThat(promptText).contains("Unique excerpt about gateway timeout");
        assertThat(promptText).contains("TKT-2002");
        assertThat(promptText).contains("ONLY");
        assertThat(promptText).doesNotContain("general knowledge");
    }

    @Test
    void mapsLlmFailuresToServiceUnavailable() {
        when(retrievalService.search("question")).thenReturn(List.of(new Document("x", "context", Map.of())));
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("ollama down"));

        assertThatThrownBy(() -> ragService.ask("question"))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("AI chat generation failed");
    }

    @Test
    void treatsModelNoMatchPhraseAsNoMatchResponse() {
        when(retrievalService.search("question"))
                .thenReturn(List.of(new Document(
                        "id",
                        "weak context",
                        Map.of(KnowledgeVectorMetadata.TICKET_ID, "TKT-1"))));
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(RagService.NO_MATCH_ANSWER)))));

        AiAskResponse response = ragService.ask("question");

        assertThat(response.noMatch()).isTrue();
        assertThat(response.citedTicketIds()).isEmpty();
        assertThat(response.retrieval().chunksUsed()).isZero();
    }
}
