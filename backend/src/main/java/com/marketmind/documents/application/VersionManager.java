package com.marketmind.documents.application;

import java.util.Optional;
import java.util.UUID;

import com.marketmind.documents.domain.DocumentVersion;

public interface VersionManager {

    DocumentVersion registerVersion(DocumentVersion candidate);

    Optional<DocumentVersion> findCurrentVersion(UUID documentId);
}
