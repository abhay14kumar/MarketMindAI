package com.marketmind.sourceintelligence.application;

public record SourceCoverageRow(
        String companySymbol,
        String documentType,
        long discoveredCount,
        long newCount,
        long ingestedCount,
        long aiReadyCount,
        String coverageStatus) {
}
