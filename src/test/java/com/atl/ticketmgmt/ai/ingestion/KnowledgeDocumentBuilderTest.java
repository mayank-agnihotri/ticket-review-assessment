package com.atl.ticketmgmt.ai.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.atl.ticketmgmt.ticket.domain.Comment;
import com.atl.ticketmgmt.ticket.domain.Priority;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeDocumentBuilderTest {

    private final KnowledgeDocumentBuilder builder = new KnowledgeDocumentBuilder();

    @Test
    void buildsDescriptionCommentsAndResolutionSections() {
        Ticket ticket = new Ticket("TKT-1001", "Checkout issue", "Card declined", Priority.HIGH, TicketStatus.OPEN);
        ticket.setCategory("payments");
        ticket.setAssignee("agent@example.com");
        ticket.setResolutionNotes("Refunded order.");

        Comment comment = new Comment(ticket, "Customer retried successfully.", "system");

        KnowledgeDocument document = builder.build(ticket, List.of(comment));

        assertThat(document.ticketId()).isEqualTo("TKT-1001");
        assertThat(document.sections()).hasSize(3);
        assertThat(document.sections().get(0).sourceType()).isEqualTo(ChunkSourceType.DESCRIPTION);
        assertThat(document.sections().get(0).text())
                .contains("Title: Checkout issue")
                .contains("Category: payments")
                .contains("Description: Card declined");
        assertThat(document.sections().get(1).sourceType()).isEqualTo(ChunkSourceType.COMMENT);
        assertThat(document.sections().get(1).text())
                .contains("Comments:")
                .contains("Customer retried successfully.");
        assertThat(document.sections().get(2).sourceType()).isEqualTo(ChunkSourceType.RESOLUTION);
        assertThat(document.sections().get(2).text()).isEqualTo("Resolution: Refunded order.");
    }

    @Test
    void omitsEmptyResolutionAndComments() {
        Ticket ticket = new Ticket("TKT-1002", "Only title", "", Priority.LOW, TicketStatus.OPEN);

        KnowledgeDocument document = builder.build(ticket, List.of());

        assertThat(document.sections()).hasSize(1);
        assertThat(document.sections().get(0).text()).isEqualTo("Title: Only title");
        assertThat(document.hasIngestibleContent()).isTrue();
    }

    @Test
    void skipsWhenNoIngestibleContent() {
        Ticket ticket = new Ticket("TKT-1003", "", "", Priority.LOW, TicketStatus.OPEN);

        KnowledgeDocument document = builder.build(ticket, List.of());

        assertThat(document.sections()).isEmpty();
        assertThat(document.hasIngestibleContent()).isFalse();
    }
}
