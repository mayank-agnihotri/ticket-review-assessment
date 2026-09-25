package com.atl.ticketmgmt.ai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiAskRequest(@NotBlank @Size(max = 2000) String question) {}
