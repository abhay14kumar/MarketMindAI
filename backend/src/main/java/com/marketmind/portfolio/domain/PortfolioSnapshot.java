package com.marketmind.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PortfolioSnapshot(
        UUID id,
        UUID portfolioId,
        UUID importJobId,
        BigDecimal totalInvestedValue,
        BigDecimal totalPresentValue,
        BigDecimal totalUnrealizedPnl,
        BigDecimal totalUnrealizedPnlPercentage,
        int totalHoldings,
        Instant capturedAt,
        Instant createdAt) {
}
