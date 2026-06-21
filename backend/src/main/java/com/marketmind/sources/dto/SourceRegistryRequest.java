package com.marketmind.sources.dto;

import java.util.Set;
import java.math.BigDecimal;

import com.marketmind.sources.domain.AuthenticationType;
import com.marketmind.sources.domain.CapabilityType;
import com.marketmind.sources.domain.RefreshFrequency;
import com.marketmind.sources.domain.SourceStatus;
import com.marketmind.sources.domain.SourceType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Source registry create or replacement request")
public record SourceRegistryRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
        String code,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 150) String organization,
        @Size(max = 1000) String description,
        @NotNull SourceType sourceType,
        @NotNull SourceStatus status,
        @NotNull AuthenticationType authenticationType,
        @NotNull RefreshFrequency refreshFrequency,
        @NotBlank
        @Size(max = 2048)
        @Pattern(
                regexp = "^https?://[^\\s]+$",
                message = "Base URL must use HTTP or HTTPS.")
        String baseUrl,
        @Size(max = 2048)
        @Pattern(
                regexp = "^$|^https?://[^\\s]+$",
                message = "Robots URL must use HTTP or HTTPS.")
        String robotsUrl,
        @Size(max = 2048)
        @Pattern(
                regexp = "^$|^https?://[^\\s]+$",
                message = "Documentation URL must use HTTP or HTTPS.")
        String documentationUrl,
        @Size(max = 2048)
        @Pattern(
                regexp = "^$|^https?://[^\\s]+$",
                message = "Sample PDF URL must use HTTP or HTTPS.")
        String samplePdfUrl,
        @NotEmpty Set<@NotNull CapabilityType> capabilities,
        boolean enabled,
        @Min(1) @Max(100) int priority,
        @NotNull @DecimalMin("0.0000") @DecimalMax("1.0000") BigDecimal reliabilityScore) {

    public SourceRegistryRequest {
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }
}
