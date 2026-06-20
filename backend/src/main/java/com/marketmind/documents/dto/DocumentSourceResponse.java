package com.marketmind.documents.dto;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.documents.domain.SourceType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Official document acquisition source")
public record DocumentSourceResponse(
        UUID id,
        @Schema(example = "NSE") String code,
        @Schema(example = "National Stock Exchange of India") String name,
        SourceType sourceType,
        @Schema(example = "https://www.nseindia.com") String baseUrl,
        boolean enabled,
        Instant lastCheckedAt,
        Instant createdAt,
        Instant updatedAt) {
}
