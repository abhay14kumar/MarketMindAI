package com.marketmind.marketdata.api;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Market-data module health and provider mode")
public record MarketHealthResponse(
        @Schema(example = "UP") String status,
        @Schema(example = "MARKETMIND_MOCK") String provider,
        @Schema(example = "MOCK") String mode,
        Instant asOf) {
}
