package com.atl.ticketmgmt.ai.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.atl.ticketmgmt.ai.support.DeterministicEmbeddingModel;
import com.atl.ticketmgmt.common.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class EmbeddingDimensionVerifierTest {

    @Test
    void failsWhenConfiguredDimensionsDoNotMatchModel() {
        AppProperties properties = new AppProperties();
        properties.getRag().setEmbeddingDimensions(512);
        var verifier = new RagVectorStoreConfiguration()
                .embeddingDimensionVerifier(properties, new DeterministicEmbeddingModel(768));

        assertThatThrownBy(() -> verifier.run(new DefaultApplicationArguments(new String[] {})))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Embedding dimensions mismatch");
    }
}
