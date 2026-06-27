package com.marketmind.discovery.infrastructure;

import java.net.URI;
import java.util.List;

import com.marketmind.discovery.application.SourceCrawler;
import com.marketmind.discovery.domain.DiscoverySourceType;

import org.springframework.stereotype.Component;

@Component
public class TestStaticCrawler implements SourceCrawler {

    private static final List<CrawledDocument> DOCUMENTS = List.of(
            new CrawledDocument(
                    URI.create("https://www.ril.com/reports/"
                            + "RIL-Integrated-Annual-Report-2024-25.pdf"),
                    "Reliance Integrated Annual Report 2024-25"),
            new CrawledDocument(
                    URI.create("https://www.w3.org/WAI/ER/tests/xhtml/"
                            + "testfiles/resources/pdf/dummy.pdf"),
                    "W3C Dummy PDF"),
            new CrawledDocument(
                    URI.create("https://example.com/investor/"
                            + "quarterly-results-q4-2025.pdf"),
                    "Quarterly Results Q4 2025"),
            new CrawledDocument(
                    URI.create("https://example.com/investor/"
                            + "investor-presentation-2025.pdf"),
                    "Investor Presentation 2025"));

    @Override
    public DiscoverySourceType sourceType() {
        return DiscoverySourceType.TEST_SOURCE;
    }

    @Override
    public CrawlResult crawl(CrawlRequest request) {
        List<CrawledDocument> documents = DOCUMENTS.stream()
                .limit(request.maxDocuments())
                .toList();
        return new CrawlResult(
                documents,
                "TEST_STATIC",
                true,
                false,
                null,
                0,
                documents.size(),
                documents.size(),
                0,
                "Test discovery completed using the deterministic public PDF corpus.");
    }
}
