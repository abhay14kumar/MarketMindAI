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
                request.organization(),
                request.description(),
                request.sourceType(),
                request.status(),
                request.authenticationType(),
                request.refreshFrequency(),
                URI.create(request.baseUrl()),
                request.robotsUrl() == null || request.robotsUrl().isBlank()
                        ? null
                        : URI.create(request.robotsUrl()),
                request.documentationUrl() == null || request.documentationUrl().isBlank()
                        ? null
                        : URI.create(request.documentationUrl()),
                request.samplePdfUrl() == null || request.samplePdfUrl().isBlank()
                        ? null
                        : URI.create(request.samplePdfUrl()),
                request.capabilities(),
                request.enabled(),
                request.priority(),
                request.reliabilityScore());
    }

    public SourceRegistryResponse toResponse(SourceRegistry source) {
        return new SourceRegistryResponse(
                source.id(),
                source.code(),
                source.name(),
                source.organization(),
                source.description(),
                source.sourceType(),
                source.status(),
                source.authenticationType(),
                source.refreshFrequency(),
                source.baseUrl().toString(),
                source.robotsUrl() == null ? null : source.robotsUrl().toString(),
                source.documentationUrl() == null ? null : source.documentationUrl().toString(),
                source.samplePdfUrl() == null ? null : source.samplePdfUrl().toString(),
                source.capabilities(),
                source.enabled(),
                source.priority(),
                source.reliabilityScore(),
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
                health.checkedAt(),
                health.lastHttpStatus(),
                health.lastLatencyMs(),
                health.robotsTxtAvailable(),
                health.robotsTxtStatus(),
                health.pdfCapabilityStatus(),
                health.lastValidatedAt());
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
                validation.sourceId(),
                validation.sourceName(),
                validation.reachable(),
                validation.httpStatus(),
                validation.latencyMs(),
                validation.robotsTxtAvailable(),
                validation.robotsTxtStatus(),
                validation.pdfCapabilityStatus(),
                validation.validationStatus(),
                validation.message(),
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
