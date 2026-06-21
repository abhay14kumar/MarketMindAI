package com.marketmind.discovery.application;

import java.net.URI;
import java.util.List;

import com.marketmind.discovery.domain.DiscoverySourceType;

public interface SourceCrawler {

    DiscoverySourceType sourceType();

    List<CrawledDocument> crawl(CrawlRequest request);

    record CrawlRequest(
            URI sourceUrl,
            String companySymbol,
            int maxDocuments) {
    }

    record CrawledDocument(
            URI documentUrl,
            String title) {
    }
}
