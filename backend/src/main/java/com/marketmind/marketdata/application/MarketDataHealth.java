package com.marketmind.marketdata.application;

import java.time.Instant;

public record MarketDataHealth(
        String status,
        String provider,
        String mode,
        Instant asOf) {
}
