package com.marketmind.marketdata.application;

import java.math.BigDecimal;

public record PortfolioPriceInput(
        String symbol,
        String isin,
        String name,
        BigDecimal averageCost,
        BigDecimal importedLastPrice,
        BigDecimal importedPreviousClose) {
}
