package com.atl.ticketmgmt.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atl.ticketmgmt.common.config.AppProperties;
import com.atl.ticketmgmt.common.exception.AiServiceUnavailableException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class TicketKnowledgeRetrievalServiceTest {

    @Mock
    private VectorStore vectorStore;

    private TicketKnowledgeRetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getRag().setTopK(4);
        properties.getRag().setSimilarityThreshold(0.72);
        retrievalService = new TicketKnowledgeRetrievalService(vectorStore, properties);
    }

    @Test
    void searchUsesConfiguredTopKAndSimilarityThreshold() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        retrievalService.search("payment failures");

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getTopK()).isEqualTo(4);
        assertThat(captor.getValue().getSimilarityThreshold()).isEqualTo(0.72);
        assertThat(captor.getValue().getQuery()).isEqualTo("payment failures");
    }

    @Test
    void searchWrapsVectorStoreFailuresAsServiceUnavailable() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> retrievalService.search("question"))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("Vector similarity search failed");
    }
}
