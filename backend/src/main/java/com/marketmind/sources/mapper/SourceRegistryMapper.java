package com.marketmind.sources.mapper;

import java.net.URI;

import com.marketmind.sources.application.PageResult;
import com.marketmind.sources.application.SourceRegistryCommand;
import com.marketmind.sources.domain.SourceCapability;
import com.marketmind.sources.domain.SourceHealth;
import com.marketmind.sources.domain.SourceRegistry;
import com.marketmind.sources.domain.SourceValidationHistory;
import com.marketmind.sources.dto.PageResponse;
import com.marketmind.sources.dto.SourceCapabilityResponse;
import com.marketmind.sources.dto.SourceHealthResponse;
import com.marketmind.sources.dto.SourceRegistryRequest;
import com.marketmind.sources.dto.SourceRegistryResponse;
import com.marketmind.sources.dto.SourceValidationResponse;

import org.springframework.stereotype.Component;

@Component
public class SourceRegistryMapper {

    public SourceRegistryCommand toCommand(SourceRegistryRequest request) {
        return new SourceRegistryCommand(
                request.code(),
                request.name(),
                request.description(),
                request.sourceType(),
                request.status(),
                request.authenticationType(),
                request.refreshFrequency(),
                URI.create(request.baseUrl()),
                request.documentationUrl() == null || request.documentationUrl().isBlank()
                        ? null
                        : URI.create(request.documentationUrl()),
                request.capabilities(),
                request.enabled());
    }

    public SourceRegistryResponse toResponse(SourceRegistry source) {
        return new SourceRegistryResponse(
                source.id(),
                source.code(),
                source.name(),
                source.description(),
                source.sourceType(),
                source.status(),
                source.authenticationType(),
                source.refreshFrequency(),
                source.baseUrl().toString(),
                source.documentationUrl() == null ? null : source.documentationUrl().toString(),
                source.capabilities(),
                source.enabled(),
                source.createdAt(),
                source.updatedAt());
    }

    public SourceHealthResponse toResponse(SourceHealth health) {
        return new SourceHealthResponse(
                health.id(),
                health.sourceId(),
                health.status(),
                health.available(),
                health.latencyMs(),
                health.message(),
                health.checkedAt());
    }

    public SourceCapabilityResponse toResponse(SourceCapability capability) {
        return new SourceCapabilityResponse(
                capability.id(),
                capability.sourceId(),
                capability.capabilityType(),
                capability.supported(),
                capability.verifiedAt());
    }

    public SourceValidationResponse toResponse(SourceValidationHistory validation) {
        return new SourceValidationResponse(
                validation.id(),
                validation.sourceId(),
                validation.validationStatus(),
                validation.available(),
                validation.latencyMs(),
                validation.message(),
                validation.supportedCapabilities(),
                validation.validatedAt());
    }

    public PageResponse<SourceRegistryResponse> toPage(PageResult<SourceRegistry> page) {
        return new PageResponse<>(
                page.content().stream().map(this::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
