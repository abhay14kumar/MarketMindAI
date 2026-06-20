package com.marketmind.sources.application;

import java.net.URI;
import java.util.Set;

import com.marketmind.sources.domain.AuthenticationType;
import com.marketmind.sources.domain.CapabilityType;
import com.marketmind.sources.domain.RefreshFrequency;
import com.marketmind.sources.domain.SourceStatus;
import com.marketmind.sources.domain.SourceType;

public record SourceRegistryCommand(
        String code,
        String name,
        String description,
        SourceType sourceType,
        SourceStatus status,
        AuthenticationType authenticationType,
        RefreshFrequency refreshFrequency,
        URI baseUrl,
        URI documentationUrl,
        Set<CapabilityType> capabilities,
        boolean enabled) {

    public SourceRegistryCommand {
        capabilities = Set.copyOf(capabilities);
    }
}
