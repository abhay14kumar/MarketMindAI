package com.marketmind.marketdata.dto;

import java.time.Instant;

import com.marketmind.marketdata.domain.PriceFeedStatus;

public record PriceProviderStatusResponse(
        String configuredProvider,
        boolean scheduledRefreshEnabled,
        long refreshIntervalSeconds,
        PriceFeedStatus lastRefreshStatus,
        int requestedSymbols,
        int successfulSymbols,
        int failedSymbols,
        String errorSummary,
        Instant lastRefreshAt) {
}
