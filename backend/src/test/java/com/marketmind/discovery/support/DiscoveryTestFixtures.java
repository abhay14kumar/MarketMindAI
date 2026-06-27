package com.marketmind.discovery.support;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.marketmind.discovery.application.DefaultDiscoveryDeduplicationService;
import com.marketmind.discovery.application.DiscoveryService;
import com.marketmind.discovery.application.KeywordDiscoveryClassificationService;
import com.marketmind.discovery.infrastructure.DiscoveryProperties;
import com.marketmind.discovery.infrastructure.GenericHtmlPdfCrawler;
import com.marketmind.discovery.infrastructure.TestStaticCrawler;
import com.marketmind.sourceintelligence.application.SourceConnectorFactory;
import com.marketmind.sourceintelligence.infrastructure.CompanyIrSourceConnector;
import com.marketmind.sourceintelligence.infrastructure.TestSourceConnector;

public final class DiscoveryTestFixtures {

    private DiscoveryTestFixtures() {
    }

    public static DiscoveryService service(InMemoryDiscoveryRepository repository) {
        DiscoveryProperties properties = new DiscoveryProperties(
                Duration.ofSeconds(1),
                1024 * 1024,
                "MarketMindAI-Test");
        TestStaticCrawler testCrawler = new TestStaticCrawler();
        GenericHtmlPdfCrawler htmlCrawler =
                new GenericHtmlPdfCrawler(HttpClient.newHttpClient(), properties);
        SourceConnectorFactory factory = new SourceConnectorFactory(java.util.List.of(
                new TestSourceConnector(testCrawler),
                new CompanyIrSourceConnector(htmlCrawler)));
        return new DiscoveryService(
                repository,
                factory,
                new DefaultDiscoveryDeduplicationService(),
                new KeywordDiscoveryClassificationService(),
                event -> {
                },
                Clock.fixed(
                        Instant.parse("2026-06-21T12:00:00Z"),
                        ZoneOffset.UTC));
    }
}
