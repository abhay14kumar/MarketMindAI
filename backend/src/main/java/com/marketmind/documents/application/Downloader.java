package com.marketmind.documents.application;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;

public interface Downloader {

    DownloadResult download(DownloadRequest request);

    record DownloadRequest(URI sourceUrl, int maxBytes) {
    }

    record DownloadResult(
            Path temporaryFile,
            String contentType,
            Instant downloadedAt,
            String originalFileName,
            long sizeBytes) {
    }
}
