package com.marketmind.discovery.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import com.marketmind.discovery.domain.DiscoveryJob;
import com.marketmind.discovery.domain.DiscoveryJobStatus;
import com.marketmind.discovery.domain.DiscoverySourceType;

import org.junit.jupiter.api.Test;

class DiscoveryMapperTest {

    @Test
    void shouldExposeReasonAndRecommendationForCompletedZeroResultJob() {
        Instant now = Instant.parse("2026-06-22T12:00:00Z");
        DiscoveryJob job = new DiscoveryJob(
                UUID.randomUUID(),
                DiscoverySourceType.NSE,
                URI.create("https://www.nseindia.com/companies-listing/corporate-filings-announcements"),
                DiscoveryJobStatus.COMPLETED,
                0,
                0,
                0,
                0,
                0,
                "Discovery completed but no documents were found.",
                "Try TEST_SOURCE for validation. An NSE-specific crawler is planned.",
                "GENERIC_HTML_PDF",
                true,
                true,
                42,
                0,
                "No direct PDF links were found in the fetched HTML.",
                null,
                now,
                now,
                now);

        var response = new DiscoveryMapper().toResponse(job);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.reasonWhenZeroResults()).contains("No direct PDF links");
        assertThat(response.recommendation()).contains("NSE-specific crawler");
    }
}
