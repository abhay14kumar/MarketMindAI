package com.marketmind.documents.domain;

import java.time.Instant;
import java.util.UUID;

public record DocumentVersion(
        UUID id,
        UUID documentId,
        int versionNumber,
        String checksumSha256,
        String storageReference,
        String mimeType,
        long sizeBytes,
        Instant acquiredAt,
        Instant createdAt) {
}
