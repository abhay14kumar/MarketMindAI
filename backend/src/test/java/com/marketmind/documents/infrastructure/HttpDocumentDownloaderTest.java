package com.marketmind.documents.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Clock;

import com.marketmind.documents.application.DocumentPipelineException;
import com.marketmind.documents.application.Downloader;

import org.junit.jupiter.api.Test;

class HttpDocumentDownloaderTest {

    private final HttpDocumentDownloader downloader = new HttpDocumentDownloader(
            new DocumentDownloadProperties(5, 1),
            Clock.systemUTC());

    @Test
    void shouldRejectUnsupportedProtocol() {
        assertThatThrownBy(() -> downloader.download(new Downloader.DownloadRequest(
                        URI.create("ftp://example.com/report.pdf"),
                        1024)))
                .isInstanceOf(DocumentPipelineException.class)
                .hasMessageContaining("HTTP");
    }

    @Test
    void shouldRejectLocalNetworkTarget() {
        assertThatThrownBy(() -> downloader.download(new Downloader.DownloadRequest(
                        URI.create("http://127.0.0.1/report.pdf"),
                        1024)))
                .isInstanceOf(DocumentPipelineException.class)
                .hasMessageContaining("private");
    }
}
