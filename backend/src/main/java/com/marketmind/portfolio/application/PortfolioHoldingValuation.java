package com.marketmind.portfolio.application;

import java.math.BigDecimal;
import java.time.Instant;

import com.marketmind.portfolio.domain.PortfolioHolding;

public record PortfolioHoldingValuation(
        PortfolioHolding holding,
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal currentValue,
        BigDecimal investedValue,
        BigDecimal totalPnl,
        BigDecimal totalPnlPercentage,
        BigDecimal dayPnl,
        BigDecimal dayPnlPercentage,
        String priceSource,
        Instant priceCapturedAt) {
}
