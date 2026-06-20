package com.marketmind.documents.api;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to queue acquisition of a document")
public record DownloadDocumentRequest(
        UUID documentId,
        @NotNull UUID sourceId,
        @NotBlank
        @Size(max = 2048)
        @Pattern(
                regexp = "^https://[^\\s]+$",
                message = "Source URL must be an HTTPS URL.")
        @Schema(example = "https://example.invalid/marketmind/annual-report-2026.pdf")
        String sourceUrl,
        @Min(1) @Max(10) int maxAttempts) {
}
