package com.marketmind.ai.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.rag")
public record RagProperties(
        int chunkSize,
        int chunkOverlap,
        int defaultTopK,
        String collectionName) {

    public RagProperties {
        chunkSize = chunkSize <= 0 ? 1000 : chunkSize;
        chunkOverlap = Math.max(0, chunkOverlap);
        defaultTopK = defaultTopK <= 0 ? 5 : defaultTopK;
        collectionName = collectionName == null || collectionName.isBlank()
                ? "marketmind_documents"
                : collectionName;
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("ai.rag.chunk-overlap must be less than chunk-size.");
        }
    }
}
