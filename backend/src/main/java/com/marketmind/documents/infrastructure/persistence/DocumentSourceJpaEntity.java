package com.marketmind.documents.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.documents.domain.SourceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_source")
class DocumentSourceJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private SourceType sourceType;

    @Column(name = "base_url", nullable = false, columnDefinition = "TEXT")
    private String baseUrl;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DocumentSourceJpaEntity() {
    }

    DocumentSourceJpaEntity(
            UUID id,
            String code,
            String name,
            SourceType sourceType,
            String baseUrl,
            boolean enabled,
            Instant lastCheckedAt,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.sourceType = sourceType;
        this.baseUrl = baseUrl;
        this.enabled = enabled;
        this.lastCheckedAt = lastCheckedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID getId() {
        return id;
    }

    String getCode() {
        return code;
    }

    String getName() {
        return name;
    }

    SourceType getSourceType() {
        return sourceType;
    }

    String getBaseUrl() {
        return baseUrl;
    }

    boolean isEnabled() {
        return enabled;
    }

    Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
