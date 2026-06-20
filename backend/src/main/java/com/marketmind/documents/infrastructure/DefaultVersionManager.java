package com.marketmind.documents.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.documents.application.DocumentCatalog;
import com.marketmind.documents.application.DocumentPipelineException;
import com.marketmind.documents.application.VersionManager;
import com.marketmind.documents.domain.DocumentVersion;

import org.springframework.stereotype.Component;

@Component
public class DefaultVersionManager implements VersionManager {

    private final DocumentCatalog documentCatalog;
    private final Clock clock;

    public DefaultVersionManager(DocumentCatalog documentCatalog, Clock clock) {
        this.documentCatalog = documentCatalog;
        this.clock = clock;
    }

    @Override
    public DocumentVersion createVersion(
            UUID documentId,
            String checksumSha256,
            String storageReference,
            String mimeType,
            long sizeBytes,
            Instant acquiredAt) {
        if (findByChecksum(checksumSha256).isPresent()) {
            throw DocumentPipelineException.duplicate(checksumSha256);
        }
        int nextVersion = documentCatalog.findVersionsByDocumentId(documentId).stream()
                .mapToInt(DocumentVersion::versionNumber)
                .max()
                .orElse(0) + 1;
        return documentCatalog.saveVersion(new DocumentVersion(
                UUID.randomUUID(),
                documentId,
                nextVersion,
                checksumSha256,
                storageReference,
                mimeType,
                sizeBytes,
                acquiredAt,
                clock.instant()));
    }

    @Override
    public Optional<DocumentVersion> findCurrentVersion(UUID documentId) {
        return documentCatalog.findVersionsByDocumentId(documentId).stream().findFirst();
    }

    @Override
    public Optional<DocumentVersion> findByChecksum(String checksumSha256) {
        return documentCatalog.findVersionByChecksum(checksumSha256);
    }

    @Override
    public List<DocumentVersion> findVersions(UUID documentId) {
        return documentCatalog.findVersionsByDocumentId(documentId);
    }
}
