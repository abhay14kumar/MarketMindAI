package com.marketmind.portfolio.domain;

import java.time.Instant;
import java.util.UUID;

public record Portfolio(
        UUID id,
        String name,
        BrokerType brokerType,
        String currency,
        Instant createdAt,
        Instant updatedAt) {
}
