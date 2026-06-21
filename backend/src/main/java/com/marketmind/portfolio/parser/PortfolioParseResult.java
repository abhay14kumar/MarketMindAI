package com.marketmind.portfolio.parser;

import java.util.List;

import com.marketmind.portfolio.domain.RowImportError;

public record PortfolioParseResult(
        int totalRows,
        List<ParsedHolding> holdings,
        List<RowImportError> errors) {

    public PortfolioParseResult {
        holdings = List.copyOf(holdings);
        errors = List.copyOf(errors);
    }
}
