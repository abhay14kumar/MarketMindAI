package com.marketmind.sourceintelligence.application;

import java.util.UUID;

public record SourceRefreshResult(
        UUID sourceId,
        UUID discoveryJobId,
        String status,
        String connectorType,
        int documentsDiscovered,
        String message,
        String recommendation) {
}
