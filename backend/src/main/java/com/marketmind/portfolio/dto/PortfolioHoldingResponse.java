package com.marketmind.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.marketmind.portfolio.domain.InstrumentType;

public record PortfolioHoldingResponse(
        UUID id,
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
        BigDecimal currentPrice,
        BigDecimal currentValue,
        BigDecimal totalPnl,
        BigDecimal totalPnlPercentage,
        BigDecimal dayPnl,
        BigDecimal dayPnlPercentage,
        String priceSource,
        Instant priceCapturedAt) {
}
