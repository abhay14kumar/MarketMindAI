package com.marketmind.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentChunkResponse(
        UUID id,
        UUID documentId,
        UUID documentVersionId,
        int chunkIndex,
        String chunkText,
        int tokenCount,
        int characterCount,
        String qdrantCollection,
        UUID qdrantPointId,
        Instant createdAt) {
}
