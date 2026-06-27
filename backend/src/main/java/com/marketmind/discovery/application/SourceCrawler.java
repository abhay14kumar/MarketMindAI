package com.marketmind.discovery.application;

import java.net.URI;
import java.util.List;

import com.marketmind.discovery.domain.DiscoverySourceType;

public interface SourceCrawler {

    DiscoverySourceType sourceType();

    CrawlResult crawl(CrawlRequest request);

    record CrawlRequest(
            URI sourceUrl,
            String companySymbol,
            int maxDocuments) {
    }

    record CrawledDocument(
            URI documentUrl,
            String title) {
    }

    record CrawlResult(
            List<CrawledDocument> documents,
            String crawlerType,
            boolean sourceReachable,
            boolean htmlFetched,
            Integer httpStatus,
            long fetchedHtmlBytes,
            int totalLinksFound,
            int pdfLinksFound,
            int skippedLinksCount,
            String message) {

        public CrawlResult {
            documents = List.copyOf(documents);
        }
    }
}
