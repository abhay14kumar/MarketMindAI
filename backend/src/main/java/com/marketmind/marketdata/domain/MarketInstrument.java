package com.marketmind.marketdata.domain;

import java.time.Instant;
import java.util.UUID;

public record MarketInstrument(
        UUID id,
        String symbol,
        String isin,
        String name,
        Exchange exchange,
        String currency,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
