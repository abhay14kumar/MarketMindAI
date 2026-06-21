package com.marketmind.discovery.application;

import java.net.URI;

import com.marketmind.discovery.domain.DiscoverySourceType;

public record DiscoveryRunCommand(
        DiscoverySourceType sourceType,
        URI sourceUrl,
        String companySymbol,
        int maxDocuments) {
}
