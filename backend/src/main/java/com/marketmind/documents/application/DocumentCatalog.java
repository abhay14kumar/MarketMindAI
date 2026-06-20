package com.marketmind.documents.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentSource;
import com.marketmind.documents.domain.DownloadJob;

public interface DocumentCatalog {

    List<Document> findAllDocuments();

    Optional<Document> findDocumentById(UUID id);

    List<DownloadJob> findAllJobs();

    Optional<DownloadJob> findJobById(UUID id);

    DownloadJob saveJob(DownloadJob job);

    List<DocumentSource> findAllSources();

    boolean existsSourceByCode(String code);

    DocumentSource saveSource(DocumentSource source);
}
