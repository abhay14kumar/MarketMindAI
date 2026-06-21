package com.marketmind.ai.domain;

import java.util.UUID;

public record Citation(
        UUID documentId,
        UUID chunkId,
        int chunkIndex,
        String snippet) {
}
