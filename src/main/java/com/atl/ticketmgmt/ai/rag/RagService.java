package com.atl.ticketmgmt.ai.rag;

import com.atl.ticketmgmt.ai.api.dto.AiAskResponse;
import com.atl.ticketmgmt.ai.api.dto.AiAskRetrievalInfo;
import com.atl.ticketmgmt.ai.vector.KnowledgeVectorMetadata;
import com.atl.ticketmgmt.common.config.AppProperties;
import com.atl.ticketmgmt.common.exception.AiServiceUnavailableException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "app.rag.vector-store", name = "type", havingValue = "pgvector", matchIfMissing = true)
public class RagService {

    public static final String NO_MATCH_ANSWER = "No relevant tickets found.";

    private final TicketKnowledgeRetrievalService retrievalService;
    private final ChatModel chatModel;
    private final AppProperties appProperties;
    private final String systemPrompt;

    public RagService(
            TicketKnowledgeRetrievalService retrievalService,
            ChatModel chatModel,
            AppProperties appProperties,
            @Value("classpath:prompts/rag-ask-system.txt") Resource systemPromptResource)
            throws IOException {
        this.retrievalService = retrievalService;
        this.chatModel = chatModel;
        this.appProperties = appProperties;
        this.systemPrompt = StreamUtils.copyToString(systemPromptResource.getInputStream(), StandardCharsets.UTF_8)
                .trim();
    }

    public AiAskResponse ask(String question) {
        AiAskRetrievalInfo retrievalConfig = retrievalConfig(0);
        try {
            List<Document> chunks = retrievalService.search(question);
            retrievalConfig = retrievalConfig(chunks.size());
            if (chunks.isEmpty()) {
                return noMatchResponse(retrievalConfig);
            }

            List<String> citedTicketIds = citedTicketIdsFrom(chunks);
            String context = formatContext(chunks);
            String answer = generateGroundedAnswer(question, context);
            if (NO_MATCH_ANSWER.equals(answer.trim())) {
                return noMatchResponse(retrievalConfig);
            }

            return new AiAskResponse(answer, citedTicketIds, true, false, retrievalConfig);
        } catch (AiServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceUnavailableException("AI retrieval or generation failed", ex);
        }
    }

    private String generateGroundedAnswer(String question, String context) {
        try {
            String userMessage = "Ticket excerpts:\n"
                    + context
                    + "\nQuestion:\n"
                    + question;
            Prompt prompt = new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)));
            ChatResponse response = chatModel.call(prompt);
            String text = response.getResult().getOutput().getText();
            if (!StringUtils.hasText(text)) {
                throw new AiServiceUnavailableException("AI model returned an empty response");
            }
            return text.trim();
        } catch (AiServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceUnavailableException("AI chat generation failed", ex);
        }
    }

    private static AiAskResponse noMatchResponse(AiAskRetrievalInfo retrieval) {
        AiAskRetrievalInfo noMatchRetrieval = new AiAskRetrievalInfo(
                retrieval.topK(), retrieval.similarityThreshold(), 0);
        return new AiAskResponse(NO_MATCH_ANSWER, List.of(), false, true, noMatchRetrieval);
    }

    private AiAskRetrievalInfo retrievalConfig(int chunksUsed) {
        return new AiAskRetrievalInfo(
                appProperties.getRag().getTopK(),
                appProperties.getRag().getSimilarityThreshold(),
                chunksUsed);
    }

    static List<String> citedTicketIdsFrom(List<Document> chunks) {
        Set<String> ordered = new LinkedHashSet<>();
        for (Document chunk : chunks) {
            Object ticketId = chunk.getMetadata().get(KnowledgeVectorMetadata.TICKET_ID);
            if (ticketId != null && StringUtils.hasText(ticketId.toString())) {
                ordered.add(ticketId.toString());
            }
        }
        return new ArrayList<>(ordered);
    }

    static String formatContext(List<Document> chunks) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < chunks.size(); index++) {
            Document chunk = chunks.get(index);
            builder.append("--- Excerpt ")
                    .append(index + 1)
                    .append(" (ticketId: ")
                    .append(chunk.getMetadata().get(KnowledgeVectorMetadata.TICKET_ID))
                    .append(") ---\n");
            builder.append(chunk.getText()).append('\n');
        }
        return builder.toString();
    }
}
