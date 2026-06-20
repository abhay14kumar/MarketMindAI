package com.marketmind.documents.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.marketmind.documents.domain.DocumentStatus;
import com.marketmind.documents.domain.DocumentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document")
class DocumentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "source_id")
    private UUID sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "reporting_period", length = 50)
    private String reportingPeriod;

    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    @Column(length = 2)
    private String quarter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentStatus status;

    @Column(name = "current_version_id")
    private UUID currentVersionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DocumentJpaEntity() {
    }

    DocumentJpaEntity(
            UUID id,
            UUID companyId,
            UUID sourceId,
            DocumentType documentType,
            String title,
            String sourceUrl,
            LocalDate publicationDate,
            String reportingPeriod,
            Integer fiscalYear,
            String quarter,
            DocumentStatus status,
            UUID currentVersionId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.sourceId = sourceId;
        this.documentType = documentType;
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.publicationDate = publicationDate;
        this.reportingPeriod = reportingPeriod;
        this.fiscalYear = fiscalYear;
        this.quarter = quarter;
        this.status = status;
        this.currentVersionId = currentVersionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getCompanyId() {
        return companyId;
    }

    UUID getSourceId() {
        return sourceId;
    }

    DocumentType getDocumentType() {
        return documentType;
    }

    String getTitle() {
        return title;
    }

    String getSourceUrl() {
        return sourceUrl;
    }

    LocalDate getPublicationDate() {
        return publicationDate;
    }

    String getReportingPeriod() {
        return reportingPeriod;
    }

    Integer getFiscalYear() {
        return fiscalYear;
    }

    String getQuarter() {
        return quarter;
    }

    DocumentStatus getStatus() {
        return status;
    }

    UUID getCurrentVersionId() {
        return currentVersionId;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
