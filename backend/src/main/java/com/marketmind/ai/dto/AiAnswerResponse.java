package com.marketmind.ai.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.marketmind.ai.domain.AiAnswerStatus;

public record AiAnswerResponse(
        UUID id,
        String question,
        String answer,
        UUID documentId,
        AiAnswerStatus status,
        BigDecimal confidenceScore,
        List<CitationResponse> citations,
        Instant createdAt) {
}
