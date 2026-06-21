package com.marketmind.discovery.infrastructure;

import com.marketmind.discovery.application.SourceCrawler;
import com.marketmind.discovery.domain.DiscoverySourceType;

import org.springframework.stereotype.Component;

@Component
public class SourceCrawlerFactory {

    private final TestStaticCrawler testStaticCrawler;
    private final GenericHtmlPdfCrawler genericHtmlPdfCrawler;

    public SourceCrawlerFactory(
            TestStaticCrawler testStaticCrawler,
            GenericHtmlPdfCrawler genericHtmlPdfCrawler) {
        this.testStaticCrawler = testStaticCrawler;
        this.genericHtmlPdfCrawler = genericHtmlPdfCrawler;
    }

    public SourceCrawler getCrawler(DiscoverySourceType sourceType) {
        return sourceType == DiscoverySourceType.TEST_SOURCE
                ? testStaticCrawler
                : genericHtmlPdfCrawler;
    }
}
