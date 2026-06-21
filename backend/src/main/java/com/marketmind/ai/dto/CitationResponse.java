package com.marketmind.ai.dto;

import java.util.UUID;

public record CitationResponse(
        UUID documentId,
        UUID chunkId,
        int chunkIndex,
        String snippet) {
}
