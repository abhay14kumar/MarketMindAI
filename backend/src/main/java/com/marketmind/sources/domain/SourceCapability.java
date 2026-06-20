package com.marketmind.sources.domain;

import java.time.Instant;
import java.util.UUID;

public record SourceCapability(
        UUID id,
        UUID sourceId,
        CapabilityType capabilityType,
        boolean supported,
        Instant verifiedAt,
        Instant createdAt) {
}
