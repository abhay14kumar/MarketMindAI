package com.marketmind.sourceintelligence.domain;

import java.time.Instant;
import java.util.UUID;

public record SourceActivity(
        UUID id,
        UUID sourceId,
        SourceActivityType activityType,
        String severity,
        String title,
        String message,
        String relatedEntityType,
        UUID relatedEntityId,
        Instant occurredAt,
        Instant createdAt) {
}
