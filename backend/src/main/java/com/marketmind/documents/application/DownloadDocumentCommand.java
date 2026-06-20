package com.marketmind.documents.application;

import java.net.URI;
import java.util.UUID;

import com.marketmind.documents.domain.DocumentType;

public record DownloadDocumentCommand(
        URI sourceUrl,
        String title,
        DocumentType documentType,
        UUID companyId,
        UUID sourceId,
        Integer fiscalYear,
        String quarter) {
}
