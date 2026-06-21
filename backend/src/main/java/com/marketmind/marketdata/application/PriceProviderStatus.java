package com.marketmind.marketdata.application;

import java.time.Instant;

import com.marketmind.marketdata.domain.PriceFeedStatus;

public record PriceProviderStatus(
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
