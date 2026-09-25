package com.atl.ticketmgmt.ticket.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.atl.ticketmgmt.ticket.domain.Comment;
import com.atl.ticketmgmt.ticket.domain.Priority;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import com.atl.ticketmgmt.ticket.support.TicketPublicIdFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("repository-test")
class TicketRepositoryIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TicketPublicIdSequenceRepository sequenceRepository;

    @Test
    void persistsTicketWithOpenStatusAndOptionalResolutionNotes() {
        String publicId = TicketPublicIdFormatter.format(sequenceRepository.nextValue());
        Ticket ticket = new Ticket(
                publicId,
                "Payment failed",
                "Card declined",
                Priority.HIGH,
                TicketStatus.OPEN);
        ticket.setCategory("payments");
        ticket.setAssignee("agent@example.com");

        Ticket saved = ticketRepository.saveAndFlush(ticket);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(saved.getResolutionNotes()).isNull();
        assertThat(ticketRepository.findByTicketId(publicId)).isPresent();
    }

    @Test
    void persistsCommentLinkedToTicket() {
        String publicId = TicketPublicIdFormatter.format(sequenceRepository.nextValue());
        Ticket ticket = ticketRepository.saveAndFlush(new Ticket(
                publicId, "Title", "Description", Priority.MEDIUM, TicketStatus.OPEN));

        Comment comment = new Comment(ticket, "Customer retried successfully", "system");
        commentRepository.saveAndFlush(comment);

        assertThat(commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId()))
                .hasSize(1)
                .first()
                .extracting(Comment::getBody)
                .isEqualTo("Customer retried successfully");
    }

    @Test
    void dataSurvivesNewRepositoryRead() {
        String publicId = TicketPublicIdFormatter.format(sequenceRepository.nextValue());
        Ticket ticket = ticketRepository.saveAndFlush(new Ticket(
                publicId, "Persist", "After flush", Priority.LOW, TicketStatus.OPEN));

        ticketRepository.flush();
        assertThat(ticketRepository.findById(ticket.getId())).isPresent();
    }
}
