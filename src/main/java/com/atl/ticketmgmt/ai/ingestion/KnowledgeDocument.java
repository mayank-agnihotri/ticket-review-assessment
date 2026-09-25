package com.atl.ticketmgmt.ai.ingestion;

import java.util.List;

/**
 * Materialized knowledge for one ticket prior to chunking.
 */
public record KnowledgeDocument(String ticketId, List<KnowledgeSection> sections) {

    public boolean hasIngestibleContent() {
        return sections.stream().anyMatch(section -> section.text() != null && !section.text().isBlank());
    }
}
