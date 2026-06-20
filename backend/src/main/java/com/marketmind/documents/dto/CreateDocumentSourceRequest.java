package com.marketmind.documents.dto;

import com.marketmind.documents.domain.SourceType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register an official document source")
public record CreateDocumentSourceRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$",
                message = "Code may contain letters, numbers, periods, underscores, or hyphens.")
        @Schema(example = "NSE")
        String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull SourceType sourceType,
        @NotBlank
        @Size(max = 2048)
        @Pattern(regexp = "^https://[^\\s]+$", message = "Base URL must be an HTTPS URL.")
        String baseUrl,
        boolean enabled) {
}
