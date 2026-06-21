package com.marketmind.portfolio.application;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketPrice(
        String symbol,
        BigDecimal currentPrice,
        BigDecimal previousClose,
        String source,
        Instant capturedAt) {
}
