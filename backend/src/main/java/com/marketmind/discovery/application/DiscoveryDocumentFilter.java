package com.marketmind.discovery.application;

import com.marketmind.discovery.domain.DiscoveredDocumentStatus;
import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.discovery.domain.DiscoverySourceType;

public record DiscoveryDocumentFilter(
        DiscoveredDocumentStatus status,
        DiscoverySourceType sourceType,
        String companySymbol,
        DiscoveredDocumentType documentType) {
}
