package com.marketmind.discovery.dto;

import java.net.URI;

import com.marketmind.discovery.domain.DiscoverySourceType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to scan one trusted source for PDF links")
public record DiscoveryRunRequest(
        @NotNull DiscoverySourceType sourceType,
        URI sourceUrl,
        @Pattern(regexp = "^[A-Za-z0-9._-]{1,30}$")
        String companySymbol,
        @Min(1) @Max(100)
        Integer maxDocuments) {
}
