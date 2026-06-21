package com.marketmind.ai.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiQuestionAnswer(
        UUID id,
        String question,
        String answer,
        UUID documentId,
        AiAnswerStatus status,
        List<Citation> citations,
        BigDecimal confidenceScore,
        Instant createdAt) {

    public AiQuestionAnswer {
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
