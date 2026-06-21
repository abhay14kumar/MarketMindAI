package com.marketmind.portfolio.mapper;

import java.math.BigDecimal;
import java.util.List;

import com.marketmind.portfolio.application.Allocation;
import com.marketmind.portfolio.application.PageResult;
import com.marketmind.portfolio.application.PortfolioImportResult;
import com.marketmind.portfolio.application.PortfolioHoldingValuation;
import com.marketmind.portfolio.application.PortfolioSummary;
import com.marketmind.portfolio.domain.PortfolioHolding;
import com.marketmind.portfolio.domain.PortfolioImportJob;
import com.marketmind.portfolio.domain.PortfolioSnapshot;
import com.marketmind.portfolio.dto.AllocationResponse;
import com.marketmind.portfolio.dto.PageResponse;
import com.marketmind.portfolio.dto.PortfolioHoldingResponse;
import com.marketmind.portfolio.dto.PortfolioImportJobResponse;
import com.marketmind.portfolio.dto.PortfolioImportResponse;
import com.marketmind.portfolio.dto.PortfolioSummaryResponse;
import com.marketmind.portfolio.dto.RowImportErrorResponse;

import org.springframework.stereotype.Component;

@Component
public class PortfolioMapper {

    public PortfolioSummaryResponse toResponse(PortfolioSummary summary) {
        return new PortfolioSummaryResponse(
                summary.totalInvestedValue(),
                summary.totalPresentValue(),
                summary.totalUnrealizedPnl(),
                summary.totalUnrealizedPnlPercentage(),
                summary.totalCurrentValue(),
                summary.totalPnl(),
                summary.totalPnlPercentage(),
                summary.dayPnl(),
                summary.dayPnlPercentage(),
                summary.totalHoldings(),
                summary.lastImportedAt(),
                summary.latestPriceAt());
    }

    public PortfolioHoldingResponse toResponse(PortfolioHoldingValuation valuation) {
        PortfolioHolding holding = valuation.holding();
        return new PortfolioHoldingResponse(
                holding.id(), holding.symbol(), holding.isin(), holding.companyName(),
                holding.sector(), holding.instrumentType(), holding.quantity(),
                holding.averageCost(), holding.lastPrice(), holding.previousClose(),
                holding.investedValue(), holding.presentValue(), holding.unrealizedPnl(),
                holding.unrealizedPnlPercentage(), holding.asOf(),
                valuation.currentPrice(), valuation.currentValue(),
                valuation.totalPnl(), valuation.totalPnlPercentage(),
                valuation.dayPnl(), valuation.dayPnlPercentage(),
                valuation.priceSource(), valuation.priceCapturedAt());
    }

    public PortfolioImportJobResponse toResponse(PortfolioImportJob job) {
        List<RowImportErrorResponse> rowErrors = job.rowErrors().stream()
                .map(error -> new RowImportErrorResponse(error.rowNumber(), error.message()))
                .toList();
        return new PortfolioImportJobResponse(
                job.id(), job.brokerType(), job.originalFileName(), job.status(),
                job.totalRows(), job.importedRows(), job.rejectedRows(), rowErrors,
                job.errorMessage(), job.startedAt(), job.completedAt());
    }

    public AllocationResponse toResponse(Allocation allocation) {
        return new AllocationResponse(
                allocation.category(), allocation.presentValue(), allocation.percentage());
    }

    public PortfolioImportResponse toResponse(PortfolioImportResult result) {
        PortfolioSnapshot snapshot = result.snapshot();
        return new PortfolioImportResponse(
                toResponse(result.importJob()),
                new PortfolioSummaryResponse(
                        snapshot.totalInvestedValue(),
                        snapshot.totalPresentValue(),
                        snapshot.totalUnrealizedPnl(),
                        snapshot.totalUnrealizedPnlPercentage(),
                        snapshot.totalPresentValue(),
                        snapshot.totalUnrealizedPnl(),
                        snapshot.totalUnrealizedPnlPercentage(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        snapshot.totalHoldings(),
                        snapshot.capturedAt(),
                        null));
    }

    public PageResponse<PortfolioHoldingResponse> toHoldingPage(
            PageResult<PortfolioHoldingValuation> result) {
        return new PageResponse<>(
                result.content().stream().map(this::toResponse).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    public PageResponse<PortfolioImportJobResponse> toImportJobPage(
            PageResult<PortfolioImportJob> result) {
        return new PageResponse<>(
                result.content().stream().map(this::toResponse).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
