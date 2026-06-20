package com.marketmind.documents.application;

import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentVersion;
import com.marketmind.documents.domain.DownloadJob;

public record DocumentDownloadResult(
        DownloadJob job,
        Document document,
        DocumentVersion version) {
}
