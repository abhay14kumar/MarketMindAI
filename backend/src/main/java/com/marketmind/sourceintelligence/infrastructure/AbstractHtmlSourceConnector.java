package com.marketmind.sourceintelligence.infrastructure;

import java.util.Set;

import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.discovery.infrastructure.GenericHtmlPdfCrawler;
import com.marketmind.sourceintelligence.application.SourceConnector;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceFormat;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;

abstract class AbstractHtmlSourceConnector implements SourceConnector {

    private final GenericHtmlPdfCrawler crawler;
    private final SourceConnectorType type;
    private final SourceTrustTier trustTier;

    AbstractHtmlSourceConnector(
            GenericHtmlPdfCrawler crawler,
            SourceConnectorType type,
            SourceTrustTier trustTier) {
        this.crawler = crawler;
        this.type = type;
        this.trustTier = trustTier;
    }

    @Override
    public SourceConnectorType type() {
        return type;
    }

    @Override
    public SourceTrustTier trustTier() {
        return trustTier;
    }

    @Override
    public Set<SourceFormat> supportedFormats() {
        return Set.of(SourceFormat.HTML, SourceFormat.PDF);
    }

    @Override
    public Set<DiscoveredDocumentType> supportedDocumentTypes() {
        return Set.of(DiscoveredDocumentType.values());
    }

    @Override
    public ConnectorResult discover(ConnectorRequest request) {
        var result = crawler.crawl(new com.marketmind.discovery.application.SourceCrawler.CrawlRequest(
                request.sourceUrl(),
                request.companySymbol(),
                request.maxDocuments()));
        return new ConnectorResult(
                result.documents().stream()
                        .map(document -> new ConnectorDocument(
                                document.documentUrl(), document.title()))
                        .toList(),
                type,
                result.sourceReachable(),
                result.htmlFetched(),
                result.httpStatus(),
                result.fetchedHtmlBytes(),
                result.totalLinksFound(),
                result.pdfLinksFound(),
                result.skippedLinksCount(),
                connectorMessage(result.message()));
    }

    protected String connectorMessage(String crawlerMessage) {
        return crawlerMessage;
    }
}
