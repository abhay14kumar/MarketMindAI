package com.marketmind.marketdata.dto;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.marketdata.domain.Exchange;

public record MarketInstrumentResponse(
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
