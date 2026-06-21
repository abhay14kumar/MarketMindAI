package com.marketmind.portfolio.application;

import java.math.BigDecimal;
import java.time.Instant;

public record PortfolioSummary(
        BigDecimal totalInvestedValue,
        BigDecimal totalPresentValue,
        BigDecimal totalUnrealizedPnl,
        BigDecimal totalUnrealizedPnlPercentage,
        BigDecimal totalCurrentValue,
        BigDecimal totalPnl,
        BigDecimal totalPnlPercentage,
        BigDecimal dayPnl,
        BigDecimal dayPnlPercentage,
        int totalHoldings,
        Instant lastImportedAt,
        Instant latestPriceAt) {
}
