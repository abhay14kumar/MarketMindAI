package com.marketmind.marketdata.domain;

import java.time.Instant;
import java.util.UUID;

public record PriceFeedJob(
        UUID id,
        PriceSource source,
        String provider,
        PriceFeedStatus status,
        int requestedInstruments,
        int updatedInstruments,
        int failedInstruments,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt) {
}
