package com.marketmind.sources.dto;

import java.util.Set;

import com.marketmind.sources.domain.AuthenticationType;
import com.marketmind.sources.domain.CapabilityType;
import com.marketmind.sources.domain.RefreshFrequency;
import com.marketmind.sources.domain.SourceStatus;
import com.marketmind.sources.domain.SourceType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Source registry create or replacement request")
public record SourceRegistryRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
        String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @NotNull SourceType sourceType,
        @NotNull SourceStatus status,
        @NotNull AuthenticationType authenticationType,
        @NotNull RefreshFrequency refreshFrequency,
        @NotBlank
        @Size(max = 2048)
        @Pattern(regexp = "^https://[^\\s]+$", message = "Base URL must be an HTTPS URL.")
        String baseUrl,
        @Size(max = 2048)
        @Pattern(
                regexp = "^$|^https://[^\\s]+$",
                message = "Documentation URL must be an HTTPS URL.")
        String documentationUrl,
        @NotEmpty Set<@NotNull CapabilityType> capabilities,
        boolean enabled) {

    public SourceRegistryRequest {
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }
}
