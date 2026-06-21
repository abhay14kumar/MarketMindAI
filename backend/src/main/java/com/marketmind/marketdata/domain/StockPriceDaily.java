package com.marketmind.marketdata.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StockPriceDaily(
        UUID id,
        UUID companyId,
        ExchangeDetails exchange,
        String symbol,
        LocalDate tradingDate,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjustedClose,
        long volume,
        String currency,
        String source) {
}
