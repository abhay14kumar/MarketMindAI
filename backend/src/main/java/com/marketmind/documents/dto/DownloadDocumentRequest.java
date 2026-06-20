package com.marketmind.documents.dto;

import java.util.UUID;

import com.marketmind.documents.domain.DocumentType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to queue acquisition of an official document")
public record DownloadDocumentRequest(
        @NotBlank
        @Size(max = 2048)
        @Pattern(
                regexp = "^https?://[^\\s]+$",
                message = "Source URL must use HTTP or HTTPS.")
        @Schema(example = "https://example.invalid/marketmind/annual-report-2026.pdf")
        String sourceUrl,
        @NotBlank @Size(max = 500) String title,
        @NotNull DocumentType documentType,
        UUID companyId,
        UUID sourceId,
        @Min(1900) @Max(2200) Integer fiscalYear,
        @Pattern(regexp = "^Q[1-4]$", message = "Quarter must be Q1, Q2, Q3, or Q4.")
        String quarter) {
}
