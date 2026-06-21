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
import com.marketmind.discovery.infrastructure.SourceCrawlerFactory;
import com.marketmind.discovery.infrastructure.TestStaticCrawler;

public final class DiscoveryTestFixtures {

    private DiscoveryTestFixtures() {
    }

    public static DiscoveryService service(InMemoryDiscoveryRepository repository) {
        DiscoveryProperties properties = new DiscoveryProperties(
                Duration.ofSeconds(1),
                1024 * 1024,
                "MarketMindAI-Test");
        SourceCrawlerFactory factory = new SourceCrawlerFactory(
                new TestStaticCrawler(),
                new GenericHtmlPdfCrawler(HttpClient.newHttpClient(), properties));
        return new DiscoveryService(
                repository,
                factory,
                new DefaultDiscoveryDeduplicationService(),
                new KeywordDiscoveryClassificationService(),
                Clock.fixed(
                        Instant.parse("2026-06-21T12:00:00Z"),
                        ZoneOffset.UTC));
    }
}
