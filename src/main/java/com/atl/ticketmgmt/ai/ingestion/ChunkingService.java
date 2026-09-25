package com.atl.ticketmgmt.ai.ingestion;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ChunkingService {

    public List<String> chunkText(String text, int maxChunkChars) {
        if (!StringUtils.hasText(text) || maxChunkChars <= 0) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() <= maxChunkChars) {
                chunks.add(trimmed);
            } else {
                chunks.addAll(splitOversizedParagraph(trimmed, maxChunkChars));
            }
        }
        return chunks;
    }

    private List<String> splitOversizedParagraph(String paragraph, int maxChunkChars) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < paragraph.length()) {
            int hardEnd = Math.min(start + maxChunkChars, paragraph.length());
            int end = hardEnd;
            if (hardEnd < paragraph.length()) {
                int sentenceEnd = findLastSentenceBoundary(paragraph, start, hardEnd);
                if (sentenceEnd > start) {
                    end = sentenceEnd;
                }
            }
            String piece = paragraph.substring(start, end).trim();
            if (!piece.isEmpty()) {
                parts.add(piece);
            }
            start = end;
            while (start < paragraph.length() && Character.isWhitespace(paragraph.charAt(start))) {
                start++;
            }
        }
        return parts;
    }

    private static int findLastSentenceBoundary(String text, int start, int maxEnd) {
        for (int i = maxEnd - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                int next = i + 1;
                if (next >= text.length() || Character.isWhitespace(text.charAt(next))) {
                    return next;
                }
            }
        }
        return maxEnd;
    }
}
