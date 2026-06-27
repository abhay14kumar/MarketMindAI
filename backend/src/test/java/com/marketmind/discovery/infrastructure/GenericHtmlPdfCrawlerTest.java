package com.marketmind.discovery.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;

class GenericHtmlPdfCrawlerTest {

    @Test
    void shouldResolveRelativeLinksAndIgnoreNonPdfLinks() {
        var documents = GenericHtmlPdfCrawler.extractPdfLinks(
                URI.create("https://example.com/investors/reports/index.html"),
                """
                <html><body>
                  <a href="../pdf/annual-report.pdf">Annual Report</a>
                  <a href="/results/q4.PDF?download=true">Quarterly results</a>
                  <a href="/news.html">News</a>
                </body></html>
                """,
                20);

        assertThat(documents).hasSize(2);
        assertThat(documents.get(0).documentUrl().toString())
                .isEqualTo("https://example.com/investors/pdf/annual-report.pdf");
        assertThat(documents.get(1).documentUrl().toString())
                .isEqualTo("https://example.com/results/q4.PDF?download=true");
    }

    @Test
    void shouldReturnDiagnosticsWhenNoPdfsAreFound() {
        var result = GenericHtmlPdfCrawler.analyzeHtml(
                URI.create("https://example.com/investors"),
                "<html><body><a href=\"/news\">News</a></body></html>",
                20,
                200,
                55);

        assertThat(result.documents()).isEmpty();
        assertThat(result.sourceReachable()).isTrue();
        assertThat(result.htmlFetched()).isTrue();
        assertThat(result.totalLinksFound()).isEqualTo(1);
        assertThat(result.pdfLinksFound()).isZero();
        assertThat(result.message()).contains("no direct PDF links");
    }

    @Test
    void shouldWarnWhenNseGenericCrawlFindsNoPdfs() {
        var result = GenericHtmlPdfCrawler.analyzeHtml(
                URI.create("https://www.nseindia.com/companies-listing/corporate-filings-announcements"),
                "<html><body><a href=\"/dynamic\">Announcements</a></body></html>",
                20,
                200,
                80);

        assertThat(result.documents()).isEmpty();
        assertThat(result.message()).contains("NSE pages often require source-specific APIs");
    }
}
