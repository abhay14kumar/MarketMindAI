package com.marketmind.sources.dto;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.sources.domain.CapabilityStatus;
import com.marketmind.sources.domain.ValidationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "HTTP-based source validation result")
public record SourceValidationResponse(
        UUID sourceId,
        String sourceName,
        boolean reachable,
        Integer httpStatus,
        long latencyMs,
        boolean robotsTxtAvailable,
        Integer robotsTxtStatus,
        CapabilityStatus pdfCapabilityStatus,
        ValidationStatus validationStatus,
        String message,
        Instant validatedAt) {
}
