package com.atl.ticketmgmt.ai.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChunkingServiceTest {

    private final ChunkingService chunkingService = new ChunkingService();

    @Test
    void splitsOnParagraphBoundaries() {
        String text = "First paragraph.\n\nSecond paragraph.";

        assertThat(chunkingService.chunkText(text, 1500)).containsExactly("First paragraph.", "Second paragraph.");
    }

    @Test
    void splitsOversizedParagraphAtSentenceBoundary() {
        String sentence = "This is sentence one. ";
        StringBuilder paragraph = new StringBuilder();
        while (paragraph.length() < 1600) {
            paragraph.append(sentence);
        }

        var chunks = chunkingService.chunkText(paragraph.toString(), 1500);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(1500));
        assertThat(String.join(" ", chunks)).contains("sentence one.");
    }

    @Test
    void hardSplitsWhenNoSentenceBoundary() {
        String text = "x".repeat(1600);

        var chunks = chunkingService.chunkText(text, 1500);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).hasSize(1500);
        assertThat(chunks.get(1)).hasSize(100);
    }
}
