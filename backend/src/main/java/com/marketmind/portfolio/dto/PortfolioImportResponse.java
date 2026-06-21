package com.marketmind.portfolio.dto;

public record PortfolioImportResponse(
        PortfolioImportJobResponse importJob,
        PortfolioSummaryResponse summary) {
}
