package com.marketmind.documents.application;

import java.net.URI;
import java.util.UUID;

public record DownloadDocumentCommand(
        UUID documentId,
        UUID sourceId,
        URI sourceUrl,
        int maxAttempts) {
}
