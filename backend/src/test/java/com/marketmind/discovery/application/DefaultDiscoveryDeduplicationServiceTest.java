package com.marketmind.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;

class DefaultDiscoveryDeduplicationServiceTest {

    private final DefaultDiscoveryDeduplicationService service =
            new DefaultDiscoveryDeduplicationService();

    @Test
    void shouldNormalizeHostPortPathAndFragment() {
        String normalized = service.normalize(URI.create(
                "HTTPS://EXAMPLE.COM:443/reports/../reports/annual.pdf?year=2025#page=2"));

        assertThat(normalized)
                .isEqualTo("https://example.com/reports/annual.pdf?year=2025");
    }
}
