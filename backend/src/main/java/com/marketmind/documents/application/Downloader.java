package com.marketmind.documents.application;

import java.net.URI;
import java.time.Instant;

public interface Downloader {

    DownloadResult download(DownloadRequest request);

    record DownloadRequest(URI sourceUrl, int maxBytes) {
    }

    record DownloadResult(byte[] content, String contentType, Instant downloadedAt) {
        public DownloadResult {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
