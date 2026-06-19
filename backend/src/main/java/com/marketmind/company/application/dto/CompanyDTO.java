package com.marketmind.company.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketmind.company.domain.model.MarketCapCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "Company", description = "Listed company master data")
public record CompanyDTO(
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        UUID id,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{2}[A-Z0-9]{9}[0-9]$", message = "must be a valid 12-character ISIN")
        @Schema(example = "INE002A01018")
        String isin,

        @Size(max = 32)
        @Pattern(regexp = "^[A-Z0-9][A-Z0-9._-]*$", message = "must contain only uppercase symbol characters")
        @Schema(example = "RELIANCE")
        String nseSymbol,

        @Size(max = 32)
        @Pattern(regexp = "^[0-9A-Z][0-9A-Z._-]*$", message = "must contain only uppercase symbol characters")
        @Schema(example = "500325")
        String bseSymbol,

        @NotBlank
        @Size(max = 255)
        @Schema(example = "Reliance Industries Limited")
        String companyName,

        @Size(max = 120)
        String sector,

        @Size(max = 120)
        String industry,

        @NotNull
        MarketCapCategory marketCapCategory,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{2}$", message = "must be an ISO 3166-1 alpha-2 country code")
        @Schema(example = "IN")
        String country,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be an ISO 4217 currency code")
        @Schema(example = "INR")
        String currency,

        @Size(max = 500)
        @Pattern(
                regexp = "^https?://[^\\s]+$",
                message = "must be an absolute HTTP or HTTPS URL")
        @Schema(example = "https://www.ril.com")
        String website,

        @PastOrPresent
        LocalDate listingDate,

        @NotNull
        Boolean active,

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Instant createdAt,

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Instant updatedAt) {

    @AssertTrue(message = "at least one of nseSymbol or bseSymbol must be provided")
    @JsonIgnore
    public boolean isExchangeSymbolPresent() {
        return hasText(nseSymbol) || hasText(bseSymbol);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
