package com.marketmind.documents.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.documents.domain.DocumentVersion;

public interface VersionManager {

    DocumentVersion createVersion(
            UUID documentId,
            String checksumSha256,
            String storageReference,
            String mimeType,
            long sizeBytes,
            Instant acquiredAt);

    Optional<DocumentVersion> findCurrentVersion(UUID documentId);

    Optional<DocumentVersion> findByChecksum(String checksumSha256);

    List<DocumentVersion> findVersions(UUID documentId);
}
