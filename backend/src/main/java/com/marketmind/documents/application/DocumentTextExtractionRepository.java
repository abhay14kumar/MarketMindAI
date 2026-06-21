package com.marketmind.documents.application;

import java.util.Optional;
import java.util.UUID;

import com.marketmind.documents.domain.DocumentTextExtraction;

public interface DocumentTextExtractionRepository {

    DocumentTextExtraction save(DocumentTextExtraction extraction);

    Optional<DocumentTextExtraction> findLatestByDocumentId(UUID documentId);
}
