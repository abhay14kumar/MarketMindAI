package com.marketmind.marketdata.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceSnapshot(
        UUID id,
        UUID instrumentId,
        String symbol,
        Exchange exchange,
        BigDecimal lastPrice,
        BigDecimal previousClose,
        PriceSource source,
        Instant capturedAt,
        Instant createdAt) {
}
