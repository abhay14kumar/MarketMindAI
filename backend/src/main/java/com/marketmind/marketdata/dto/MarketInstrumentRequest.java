package com.marketmind.marketdata.dto;

import com.marketmind.marketdata.domain.Exchange;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MarketInstrumentRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9.-]{0,63}$")
        String symbol,
        String isin,
        String name,
        @NotNull Exchange exchange,
        @Pattern(regexp = "^[A-Za-z]{3}$") String currency) {
}
