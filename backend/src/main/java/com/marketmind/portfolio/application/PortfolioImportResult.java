package com.marketmind.portfolio.application;

import com.marketmind.portfolio.domain.PortfolioImportJob;
import com.marketmind.portfolio.domain.PortfolioSnapshot;

public record PortfolioImportResult(
        PortfolioImportJob importJob,
        PortfolioSnapshot snapshot) {
}
