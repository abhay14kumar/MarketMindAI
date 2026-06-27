package com.marketmind.sourceintelligence.application;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceFormat;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;
import com.marketmind.sources.domain.CapabilityType;

public record SourceCatalogItem(
        UUID id,
        String code,
        String name,
        String organization,
        String sourceType,
        String status,
        URI baseUrl,
        boolean official,
        int priority,
        BigDecimal reliabilityScore,
        int trustScore,
        int freshnessScore,
        SourceTrustTier trustTier,
        SourceConnectorType connectorType,
        Set<CapabilityType> capabilities,
        Set<SourceFormat> supportedFormats,
        Set<DiscoveredDocumentType> supportedDocumentTypes,
        Boolean healthy,
        Long latencyMs,
        Integer httpStatus,
        Instant lastValidatedAt,
        Instant lastCrawlAt,
        Instant nextCrawlAt,
        String schedulerState,
        long totalCrawls,
        long successfulCrawls,
        long failedCrawls,
        long documentsDiscovered) {
}
