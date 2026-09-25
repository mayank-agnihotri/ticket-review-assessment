package com.atl.ticketmgmt.ai.config;

import com.atl.ticketmgmt.common.config.AppProperties;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagVectorStoreConfiguration {

    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    @ConditionalOnProperty(prefix = "app.rag.vector-store", name = "type", havingValue = "pgvector", matchIfMissing = true)
    ApplicationRunner embeddingDimensionVerifier(AppProperties appProperties, EmbeddingModel embeddingModel) {
        return args -> {
            int configured = appProperties.getRag().getEmbeddingDimensions();
            int actual = embeddingModel.dimensions();
            if (actual != configured) {
                throw new IllegalStateException(
                        "Embedding dimensions mismatch: configured app.rag.embedding-dimensions="
                                + configured
                                + " but EmbeddingModel reports "
                                + actual
                                + ". Align spring.ai.vectorstore.pgvector.dimensions with the embedding model.");
            }
        };
    }
}
