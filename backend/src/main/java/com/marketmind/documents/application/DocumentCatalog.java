package com.marketmind.documents.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.net.URI;

import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentSource;
import com.marketmind.documents.domain.DocumentVersion;
import com.marketmind.documents.domain.DownloadJob;

public interface DocumentCatalog {

    List<Document> findAllDocuments();

    Optional<Document> findDocumentById(UUID id);

    Optional<Document> findDocumentBySourceUrl(URI sourceUrl);

    Document saveDocument(Document document);

    List<DownloadJob> findAllJobs();

    Optional<DownloadJob> findJobById(UUID id);

    DownloadJob saveJob(DownloadJob job);

    Optional<DocumentSource> findSourceById(UUID id);

    List<DocumentSource> findAllSources();

    boolean existsSourceByCode(String code);

    DocumentSource saveSource(DocumentSource source);

    Optional<DocumentVersion> findVersionByChecksum(String checksumSha256);

    List<DocumentVersion> findVersionsByDocumentId(UUID documentId);

    DocumentVersion saveVersion(DocumentVersion version);
}
