package com.marketmind.marketdata.dto;

import java.math.BigDecimal;

import com.marketmind.marketdata.domain.Exchange;
import com.marketmind.marketdata.domain.PriceSource;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ManualPriceRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9.-]{0,63}$")
        String symbol,
        @NotNull Exchange exchange,
        @NotNull @DecimalMin("0.0") BigDecimal lastPrice,
        @NotNull @DecimalMin("0.0") BigDecimal previousClose,
        @NotNull PriceSource source) {
}
