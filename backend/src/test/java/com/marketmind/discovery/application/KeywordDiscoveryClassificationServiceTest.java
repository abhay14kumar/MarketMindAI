package com.marketmind.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import com.marketmind.discovery.domain.DiscoveredDocumentType;

import org.junit.jupiter.api.Test;

class KeywordDiscoveryClassificationServiceTest {

    private final KeywordDiscoveryClassificationService service =
            new KeywordDiscoveryClassificationService();

    @Test
    void shouldClassifySupportedDocumentKeywords() {
        assertThat(service.classify(
                "Integrated Annual Report",
                URI.create("https://example.com/report.pdf")))
                .isEqualTo(DiscoveredDocumentType.ANNUAL_REPORT);
        assertThat(service.classify(
                "Results",
                URI.create("https://example.com/financial-results.pdf")))
                .isEqualTo(DiscoveredDocumentType.QUARTERLY_RESULT);
        assertThat(service.classify(
                "Investor Presentation",
                URI.create("https://example.com/document.pdf")))
                .isEqualTo(DiscoveredDocumentType.INVESTOR_PRESENTATION);
        assertThat(service.classify(
                "Document",
                URI.create("https://example.com/circular-42.pdf")))
                .isEqualTo(DiscoveredDocumentType.CIRCULAR);
        assertThat(service.classify(
                "Exchange Announcement",
                URI.create("https://example.com/file.pdf")))
                .isEqualTo(DiscoveredDocumentType.ANNOUNCEMENT);
    }
}
