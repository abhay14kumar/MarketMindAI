package com.marketmind.marketdata.application;

import java.util.Locale;

import com.marketmind.marketdata.domain.Exchange;

import org.springframework.stereotype.Component;

@Component
public class SymbolMapper {

    public String toProviderSymbol(String symbol, Exchange exchange) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol must not be blank.");
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if (normalized.endsWith(".NS") || normalized.endsWith(".BO")) {
            return normalized;
        }
        return switch (exchange) {
            case BSE -> normalized + ".BO";
            case NSE, UNKNOWN -> normalized + ".NS";
        };
    }
}
