package com.marketmind.marketdata.domain;

import java.time.Instant;
import java.util.UUID;

public record ExchangeDetails(
        UUID id,
        String code,
        String name,
        String country,
        String currency,
        String timeZone,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
