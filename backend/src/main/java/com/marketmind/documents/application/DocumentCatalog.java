package com.marketmind.documents.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DownloadJob;

public interface DocumentCatalog {

    List<Document> findAllDocuments();

    Optional<Document> findDocumentById(UUID id);

    List<DownloadJob> findAllJobs();

    DownloadJob saveJob(DownloadJob job);
}
