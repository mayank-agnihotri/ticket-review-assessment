package com.atl.ticketmgmt.ai.api;

import com.atl.ticketmgmt.ai.api.dto.AiAskRequest;
import com.atl.ticketmgmt.ai.api.dto.AiAskResponse;
import com.atl.ticketmgmt.ai.rag.RagService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@ConditionalOnBean(RagService.class)
public class AiAskController {

    private final RagService ragService;

    public AiAskController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public AiAskResponse ask(@Valid @RequestBody AiAskRequest request) {
        return ragService.ask(request.question());
    }
}
