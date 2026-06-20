package com.marketmind.documents.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Immutable acquired document version")
public record DocumentVersionResponse(
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
