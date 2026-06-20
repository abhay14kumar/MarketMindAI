package com.marketmind.marketdata.api;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Latest mock market index observation")
public record MarketIndexResponse(
        @Schema(example = "NIFTY50") String symbol,
        @Schema(example = "NIFTY 50") String name,
        @Schema(example = "NSE") String exchange,
        @Schema(example = "24853.40") BigDecimal lastValue,
        @Schema(example = "126.15") BigDecimal changeValue,
        @Schema(example = "0.51") BigDecimal changePercent,
        @Schema(example = "INR") String currency,
        Instant asOf) {
}
