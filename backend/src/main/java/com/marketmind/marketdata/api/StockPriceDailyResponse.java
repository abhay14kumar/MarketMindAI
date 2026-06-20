package com.marketmind.marketdata.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Daily OHLCV stock-price observation")
public record StockPriceDailyResponse(
        @Schema(example = "RELIANCE") String symbol,
        @Schema(example = "NSE") String exchange,
        LocalDate tradingDate,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjustedClose,
        @Schema(example = "7850000") long volume,
        @Schema(example = "INR") String currency,
        @Schema(example = "MARKETMIND_MOCK") String source) {
}
