package com.marketmind.marketdata.application;

import java.math.BigDecimal;
import java.time.Instant;

import com.marketmind.marketdata.domain.PriceSource;

public record PriceProviderResult(
        String requestedSymbol,
        String providerSymbol,
        BigDecimal lastPrice,
        BigDecimal previousClose,
        String currency,
        PriceSource source,
        Instant providerTimestamp) {
}
