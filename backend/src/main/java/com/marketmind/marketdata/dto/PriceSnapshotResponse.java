package com.marketmind.marketdata.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.marketmind.marketdata.domain.Exchange;
import com.marketmind.marketdata.domain.PriceSource;

public record PriceSnapshotResponse(
        UUID id,
        String symbol,
        Exchange exchange,
        BigDecimal lastPrice,
        BigDecimal previousClose,
        PriceSource source,
        Instant capturedAt) {
}
