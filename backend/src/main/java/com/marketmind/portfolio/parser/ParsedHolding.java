package com.marketmind.portfolio.parser;

import java.math.BigDecimal;

import com.marketmind.portfolio.domain.InstrumentType;

public record ParsedHolding(
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
        BigDecimal unrealizedPnlPercentage) {
}
