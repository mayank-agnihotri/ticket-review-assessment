package com.atl.ticketmgmt.ai.ingestion;

import com.atl.ticketmgmt.ticket.domain.Comment;
import com.atl.ticketmgmt.ticket.domain.Ticket;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KnowledgeDocumentBuilder {

    public KnowledgeDocument build(Ticket ticket, List<Comment> comments) {
        List<KnowledgeSection> sections = new ArrayList<>();

        String descriptionBlock = buildDescriptionBlock(ticket);
        if (StringUtils.hasText(descriptionBlock)) {
            sections.add(new KnowledgeSection(ChunkSourceType.DESCRIPTION, descriptionBlock));
        }

        String commentsBlock = buildCommentsBlock(comments);
        if (StringUtils.hasText(commentsBlock)) {
            sections.add(new KnowledgeSection(ChunkSourceType.COMMENT, commentsBlock));
        }

        if (StringUtils.hasText(ticket.getResolutionNotes())) {
            sections.add(new KnowledgeSection(
                    ChunkSourceType.RESOLUTION, "Resolution: " + ticket.getResolutionNotes().trim()));
        }

        return new KnowledgeDocument(ticket.getTicketId(), sections);
    }

    private static String buildDescriptionBlock(Ticket ticket) {
        StringBuilder block = new StringBuilder();
        if (StringUtils.hasText(ticket.getTitle())) {
            block.append("Title: ").append(ticket.getTitle().trim()).append('\n');
        }
        if (StringUtils.hasText(ticket.getCategory())) {
            block.append("Category: ").append(ticket.getCategory().trim()).append('\n');
        }
        if (StringUtils.hasText(ticket.getDescription())) {
            block.append("Description: ").append(ticket.getDescription().trim());
        }
        return block.toString().trim();
    }

    private static String buildCommentsBlock(List<Comment> comments) {
        if (comments.isEmpty()) {
            return "";
        }
        StringBuilder block = new StringBuilder("Comments:\n");
        for (Comment comment : comments) {
            block.append("- [")
                    .append(comment.getCreatedAt())
                    .append("] ")
                    .append(comment.getBody().trim())
                    .append('\n');
        }
        return block.toString().trim();
    }
}
