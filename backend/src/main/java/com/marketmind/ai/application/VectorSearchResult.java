package com.marketmind.ai.application;

import java.util.UUID;

public record VectorSearchResult(
        UUID documentId,
        UUID chunkId,
        int chunkIndex,
        String chunkText,
        String title,
        double score) {
}
