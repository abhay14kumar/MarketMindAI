package com.marketmind.sources.application;

import java.net.URI;
import java.math.BigDecimal;
import java.util.Set;

import com.marketmind.sources.domain.AuthenticationType;
import com.marketmind.sources.domain.CapabilityType;
import com.marketmind.sources.domain.RefreshFrequency;
import com.marketmind.sources.domain.SourceStatus;
import com.marketmind.sources.domain.SourceType;

public record SourceRegistryCommand(
        String code,
        String name,
        String organization,
        String description,
        SourceType sourceType,
        SourceStatus status,
        AuthenticationType authenticationType,
        RefreshFrequency refreshFrequency,
        URI baseUrl,
        URI robotsUrl,
        URI documentationUrl,
        URI samplePdfUrl,
        Set<CapabilityType> capabilities,
        boolean enabled,
        int priority,
        BigDecimal reliabilityScore) {

    public SourceRegistryCommand {
        capabilities = Set.copyOf(capabilities);
    }
}
