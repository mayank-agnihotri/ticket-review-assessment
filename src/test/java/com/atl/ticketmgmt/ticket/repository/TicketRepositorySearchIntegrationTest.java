package com.atl.ticketmgmt.ticket.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.atl.ticketmgmt.ticket.domain.Comment;
import com.atl.ticketmgmt.ticket.domain.Priority;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import com.atl.ticketmgmt.ticket.support.TicketPublicIdFormatter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("repository-test")
class TicketRepositorySearchIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TicketPublicIdSequenceRepository sequenceRepository;

    @Test
    void findForListFiltersByQueryAndStatusWithoutMatchingComments() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        Ticket inTitle = saveTicket("AlphaSearchKey" + suffix, "desc", TicketStatus.OPEN);
        Ticket inDescription = saveTicket("title", "BetaSearchKey" + suffix + " text", TicketStatus.IN_PROGRESS);
        Ticket commentOnly = saveTicket("title2", "desc2", TicketStatus.OPEN);
        commentRepository.saveAndFlush(
                new Comment(commentOnly, "GammaSearchKey" + suffix + " in comment", "system"));

        var page = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "updatedAt"));

        assertThat(ticketRepository.findForList("AlphaSearchKey" + suffix, null, page).getContent())
                .extracting(Ticket::getTicketId)
                .contains(inTitle.getTicketId());

        assertThat(ticketRepository.findForList("BetaSearchKey" + suffix, null, page).getContent())
                .extracting(Ticket::getTicketId)
                .contains(inDescription.getTicketId());

        assertThat(ticketRepository.findForList("GammaSearchKey" + suffix, null, page).getContent())
                .isEmpty();

        assertThat(ticketRepository.findForList(null, TicketStatus.IN_PROGRESS, page).getContent())
                .extracting(Ticket::getTicketId)
                .contains(inDescription.getTicketId());
    }

    private Ticket saveTicket(String title, String description, TicketStatus status) {
        String publicId = TicketPublicIdFormatter.format(sequenceRepository.nextValue());
        Ticket ticket = new Ticket(publicId, title, description, Priority.LOW, status);
        return ticketRepository.saveAndFlush(ticket);
    }
}
