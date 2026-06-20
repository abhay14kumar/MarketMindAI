package com.marketmind.documents.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Completed document acquisition result")
public record DocumentDownloadResponse(
        DownloadJobResponse job,
        DocumentResponse document,
        DocumentVersionResponse version) {
}
