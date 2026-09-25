package com.atl.ticketmgmt.ai.support;

import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * PGVector integration tests without Ollama: deterministic embeddings and a stub chat model.
 */
@TestConfiguration
public class DeterministicPgVectorTestConfiguration {

    @Bean
    @Primary
    EmbeddingModel testEmbeddingModel() {
        return new DeterministicEmbeddingModel(768);
    }

    @Bean
    @Primary
    ChatModel testChatModel() {
        return prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage("stub"))));
    }
}
