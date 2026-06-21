package com.marketmind.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PortfolioHolding(
        UUID id,
        UUID portfolioId,
        UUID importJobId,
        String symbol,
        String isin,
        String companyName,
        String sector,
        InstrumentType instrumentType,
        BigDecimal quantity,
        BigDecimal averageCost,
        BigDecimal lastPrice,
        BigDecimal previousClose,
        BigDecimal investedValue,
        BigDecimal presentValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPercentage,
        Instant asOf,
        Instant createdAt,
        Instant updatedAt) {
}
