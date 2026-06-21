package com.marketmind.marketdata.dto;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.marketdata.domain.PriceFeedStatus;
import com.marketmind.marketdata.domain.PriceSource;

public record PriceFeedJobResponse(
        UUID id,
        PriceSource source,
        String provider,
        PriceFeedStatus status,
        int requestedInstruments,
        int updatedInstruments,
        int failedInstruments,
        String errorMessage,
        Instant startedAt,
        Instant completedAt) {
}
