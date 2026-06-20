package com.marketmind.documents.domain;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record DocumentSource(
        UUID id,
        String code,
        String name,
        String sourceType,
        URI baseUrl,
        boolean enabled,
        Instant lastCheckedAt,
        Instant createdAt,
        Instant updatedAt) {
}
