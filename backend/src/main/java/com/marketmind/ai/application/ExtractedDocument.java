package com.marketmind.ai.application;

import java.util.UUID;

public record ExtractedDocument(
        UUID documentId,
        UUID documentVersionId,
        String title,
        String extractedText) {
}
