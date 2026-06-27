package com.marketmind.sourceintelligence.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.marketmind.discovery.domain.DiscoveredDocumentType;

public record SourceIntelligenceProfile(
        UUID sourceId,
        SourceConnectorType connectorType,
        SourceTrustTier trustTier,
        int trustScore,
        int freshnessScore,
        Set<SourceFormat> supportedFormats,
        Set<DiscoveredDocumentType> supportedDocumentTypes,
        Instant lastCrawlAt,
        Instant nextCrawlAt,
        String schedulerState,
        long totalCrawls,
        long successfulCrawls,
        long failedCrawls,
        long documentsDiscovered,
        Instant createdAt,
        Instant updatedAt) {

    public SourceIntelligenceProfile {
        supportedFormats = Set.copyOf(supportedFormats);
        supportedDocumentTypes = Set.copyOf(supportedDocumentTypes);
    }
}
