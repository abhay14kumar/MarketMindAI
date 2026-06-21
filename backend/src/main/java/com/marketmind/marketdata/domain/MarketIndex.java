package com.marketmind.marketdata.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MarketIndex(
        UUID id,
        ExchangeDetails exchange,
        String symbol,
        String name,
        String currency,
        BigDecimal lastValue,
        BigDecimal changeValue,
        BigDecimal changePercent,
        Instant asOf,
        boolean active) {
}
