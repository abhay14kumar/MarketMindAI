package com.marketmind.ai.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiAskRequest(
        @NotBlank @Size(max = 4000) String question,
        UUID documentId,
        @Min(1) @Max(20) Integer topK) {
}
