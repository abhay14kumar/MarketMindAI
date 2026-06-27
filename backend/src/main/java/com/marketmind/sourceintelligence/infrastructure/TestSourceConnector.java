package com.marketmind.sourceintelligence.infrastructure;

import java.util.Set;

import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.discovery.infrastructure.TestStaticCrawler;
import com.marketmind.sourceintelligence.application.SourceConnector;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceFormat;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;
import org.springframework.stereotype.Component;

@Component
public class TestSourceConnector implements SourceConnector {
    private final TestStaticCrawler crawler;
    public TestSourceConnector(TestStaticCrawler crawler) { this.crawler = crawler; }
    @Override public SourceConnectorType type() { return SourceConnectorType.TEST_STATIC; }
    @Override public SourceTrustTier trustTier() { return SourceTrustTier.TEST; }
    @Override public Set<SourceFormat> supportedFormats() { return Set.of(SourceFormat.PDF); }
    @Override public Set<DiscoveredDocumentType> supportedDocumentTypes() {
        return Set.of(DiscoveredDocumentType.values());
    }
    @Override public int selectionScore(ConnectorRequest request) {
        return request.sourceType() == DiscoverySourceType.TEST_SOURCE ? 2000 : 0;
    }
    @Override public ConnectorResult discover(ConnectorRequest request) {
        var result = crawler.crawl(new com.marketmind.discovery.application.SourceCrawler.CrawlRequest(
                request.sourceUrl(), request.companySymbol(), request.maxDocuments()));
        return new ConnectorResult(
                result.documents().stream()
                        .map(document -> new ConnectorDocument(document.documentUrl(), document.title()))
                        .toList(),
                type(), true, false, null, 0,
                result.totalLinksFound(), result.pdfLinksFound(),
                result.skippedLinksCount(), result.message());
    }
}
