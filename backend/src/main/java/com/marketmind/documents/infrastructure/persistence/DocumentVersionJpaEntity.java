package com.marketmind.documents.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_version")
class DocumentVersionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "storage_reference", columnDefinition = "TEXT")
    private String storageReference;

    @Column(name = "mime_type", nullable = false, length = 150)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "acquired_at", nullable = false)
    private Instant acquiredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DocumentVersionJpaEntity() {
    }

    DocumentVersionJpaEntity(
            UUID id,
            UUID documentId,
            int versionNumber,
            String checksumSha256,
            String storageReference,
            String mimeType,
            long sizeBytes,
            Instant acquiredAt,
            Instant createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.versionNumber = versionNumber;
        this.checksumSha256 = checksumSha256;
        this.storageReference = storageReference;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.acquiredAt = acquiredAt;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    UUID getDocumentId() { return documentId; }
    int getVersionNumber() { return versionNumber; }
    String getChecksumSha256() { return checksumSha256; }
    String getStorageReference() { return storageReference; }
    String getMimeType() { return mimeType; }
    long getSizeBytes() { return sizeBytes; }
    Instant getAcquiredAt() { return acquiredAt; }
    Instant getCreatedAt() { return createdAt; }
}
