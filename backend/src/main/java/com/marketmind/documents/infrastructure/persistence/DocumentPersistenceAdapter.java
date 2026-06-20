package com.marketmind.documents.infrastructure.persistence;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.documents.application.DocumentCatalog;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentSource;
import com.marketmind.documents.domain.DocumentVersion;
import com.marketmind.documents.domain.DownloadJob;

import org.springframework.stereotype.Component;

@Component
public class DocumentPersistenceAdapter implements DocumentCatalog {

    private final SpringDataDocumentRepository documentRepository;
    private final SpringDataDocumentSourceRepository sourceRepository;
    private final SpringDataDownloadJobRepository jobRepository;
    private final SpringDataDocumentVersionRepository versionRepository;

    public DocumentPersistenceAdapter(
            SpringDataDocumentRepository documentRepository,
            SpringDataDocumentSourceRepository sourceRepository,
            SpringDataDownloadJobRepository jobRepository,
            SpringDataDocumentVersionRepository versionRepository) {
        this.documentRepository = documentRepository;
        this.sourceRepository = sourceRepository;
        this.jobRepository = jobRepository;
        this.versionRepository = versionRepository;
    }

    @Override
    public List<Document> findAllDocuments() {
        return documentRepository.findAll().stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(
                        Document::publicationDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public Optional<Document> findDocumentById(UUID id) {
        return documentRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Document> findDocumentBySourceUrl(URI sourceUrl) {
        return documentRepository.findBySourceUrl(sourceUrl.toString()).map(this::toDomain);
    }

    @Override
    public Document saveDocument(Document document) {
        return toDomain(documentRepository.save(toEntity(document)));
    }

    @Override
    public List<DownloadJob> findAllJobs() {
        return jobRepository.findAll().stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(DownloadJob::submittedAt).reversed())
                .toList();
    }

    @Override
    public Optional<DownloadJob> findJobById(UUID id) {
        return jobRepository.findById(id).map(this::toDomain);
    }

    @Override
    public DownloadJob saveJob(DownloadJob job) {
        return toDomain(jobRepository.save(toEntity(job)));
    }

    @Override
    public Optional<DocumentSource> findSourceById(UUID id) {
        return sourceRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<DocumentSource> findAllSources() {
        return sourceRepository.findAll().stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(DocumentSource::code))
                .toList();
    }

    @Override
    public boolean existsSourceByCode(String code) {
        return sourceRepository.existsByCodeIgnoreCase(code);
    }

    @Override
    public DocumentSource saveSource(DocumentSource source) {
        return toDomain(sourceRepository.save(toEntity(source)));
    }

    @Override
    public Optional<DocumentVersion> findVersionByChecksum(String checksumSha256) {
        return versionRepository.findFirstByChecksumSha256IgnoreCase(checksumSha256)
                .map(this::toDomain);
    }

    @Override
    public List<DocumentVersion> findVersionsByDocumentId(UUID documentId) {
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public DocumentVersion saveVersion(DocumentVersion version) {
        return toDomain(versionRepository.save(toEntity(version)));
    }

    private Document toDomain(DocumentJpaEntity entity) {
        DocumentSource source = entity.getSourceId() == null
                ? null
                : sourceRepository.findById(entity.getSourceId()).map(this::toDomain).orElse(null);
        return new Document(
                entity.getId(),
                entity.getCompanyId(),
                source,
                entity.getDocumentType(),
                entity.getTitle(),
                entity.getSourceUrl() == null ? null : URI.create(entity.getSourceUrl()),
                entity.getPublicationDate(),
                entity.getReportingPeriod(),
                entity.getFiscalYear(),
                entity.getQuarter(),
                entity.getStatus(),
                entity.getCurrentVersionId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private DocumentJpaEntity toEntity(Document document) {
        return new DocumentJpaEntity(
                document.id(),
                document.companyId(),
                document.source() == null ? null : document.source().id(),
                document.documentType(),
                document.title(),
                document.sourceUrl() == null ? null : document.sourceUrl().toString(),
                document.publicationDate(),
                document.reportingPeriod(),
                document.fiscalYear(),
                document.quarter(),
                document.status(),
                document.currentVersionId(),
                document.createdAt(),
                document.updatedAt());
    }

    private DocumentSource toDomain(DocumentSourceJpaEntity entity) {
        return new DocumentSource(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getSourceType(),
                URI.create(entity.getBaseUrl()),
                entity.isEnabled(),
                entity.getLastCheckedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private DocumentSourceJpaEntity toEntity(DocumentSource source) {
        return new DocumentSourceJpaEntity(
                source.id(),
                source.code(),
                source.name(),
                source.sourceType(),
                source.baseUrl().toString(),
                source.enabled(),
                source.lastCheckedAt(),
                source.createdAt(),
                source.updatedAt());
    }

    private DownloadJob toDomain(DownloadJobJpaEntity entity) {
        return new DownloadJob(
                entity.getId(),
                entity.getDocumentId(),
                entity.getSourceId(),
                URI.create(entity.getRequestedUrl()),
                entity.getStatus(),
                entity.getAttemptCount(),
                entity.getMaxAttempts(),
                entity.getRetryOfJobId(),
                entity.getSubmittedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getNextAttemptAt(),
                entity.getErrorCode(),
                entity.getErrorMessage());
    }

    private DownloadJobJpaEntity toEntity(DownloadJob job) {
        return new DownloadJobJpaEntity(
                job.id(),
                job.documentId(),
                job.sourceId(),
                job.requestedUrl().toString(),
                job.status(),
                job.attemptCount(),
                job.maxAttempts(),
                job.retryOfJobId(),
                job.submittedAt(),
                job.startedAt(),
                job.completedAt(),
                job.nextAttemptAt(),
                job.errorCode(),
                job.errorMessage());
    }

    private DocumentVersion toDomain(DocumentVersionJpaEntity entity) {
        return new DocumentVersion(
                entity.getId(),
                entity.getDocumentId(),
                entity.getVersionNumber(),
                entity.getChecksumSha256(),
                entity.getStorageReference(),
                entity.getMimeType(),
                entity.getSizeBytes(),
                entity.getAcquiredAt(),
                entity.getCreatedAt());
    }

    private DocumentVersionJpaEntity toEntity(DocumentVersion version) {
        return new DocumentVersionJpaEntity(
                version.id(),
                version.documentId(),
                version.versionNumber(),
                version.checksumSha256(),
                version.storageReference(),
                version.mimeType(),
                version.sizeBytes(),
                version.acquiredAt(),
                version.createdAt());
    }
}
