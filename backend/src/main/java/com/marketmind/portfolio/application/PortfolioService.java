package com.marketmind.portfolio.application;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.marketmind.common.exception.ApiException;
import com.marketmind.common.exception.ErrorCode;
import com.marketmind.portfolio.domain.BrokerType;
import com.marketmind.portfolio.domain.ImportStatus;
import com.marketmind.portfolio.domain.InstrumentType;
import com.marketmind.portfolio.domain.Portfolio;
import com.marketmind.portfolio.domain.PortfolioHolding;
import com.marketmind.portfolio.domain.PortfolioImportJob;
import com.marketmind.portfolio.domain.PortfolioSnapshot;
import com.marketmind.portfolio.parser.ParsedHolding;
import com.marketmind.portfolio.parser.PortfolioFileParser;
import com.marketmind.portfolio.parser.PortfolioParseResult;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

    private static final String DEFAULT_PORTFOLIO_NAME = "Zerodha Holdings";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final PortfolioRepository repository;
    private final PortfolioFileParser parser;
    private final PortfolioImportTransactions importTransactions;
    private final MarketPriceReader marketPriceReader;

    public PortfolioService(
            PortfolioRepository repository,
            PortfolioFileParser parser,
            PortfolioImportTransactions importTransactions,
            MarketPriceReader marketPriceReader) {
        this.repository = repository;
        this.parser = parser;
        this.importTransactions = importTransactions;
        this.marketPriceReader = marketPriceReader;
    }

    public PortfolioImportResult importHoldings(String originalFileName, InputStream input) {
        Portfolio portfolio = repository.getOrCreatePortfolio(
                BrokerType.ZERODHA, DEFAULT_PORTFOLIO_NAME);
        Instant startedAt = Instant.now();
        UUID jobId = UUID.randomUUID();
        PortfolioImportJob startedJob = new PortfolioImportJob(
                jobId, portfolio.id(), BrokerType.ZERODHA, originalFileName,
                ImportStatus.STARTED, 0, 0, 0, List.of(), null,
                startedAt, null, startedAt);
        importTransactions.start(startedJob);

        PortfolioParseResult parsed = null;
        try {
            parsed = parser.parse(input);
            if (parsed.holdings().isEmpty()) {
                throw new IllegalArgumentException(
                        "The workbook does not contain any valid holdings.");
            }

            Instant now = Instant.now();
            List<PortfolioHolding> holdings = parsed.holdings().stream()
                    .map(holding -> toHolding(holding, portfolio.id(), jobId, now))
                    .toList();
            PortfolioSnapshot snapshot = createSnapshot(portfolio.id(), jobId, holdings, now);
            PortfolioImportJob completedJob = new PortfolioImportJob(
                    jobId, portfolio.id(), BrokerType.ZERODHA, originalFileName,
                    ImportStatus.COMPLETED, parsed.totalRows(), holdings.size(),
                    parsed.errors().size(), parsed.errors(), null,
                    startedAt, now, startedAt);
            importTransactions.complete(portfolio.id(), holdings, snapshot, completedJob);
            return new PortfolioImportResult(completedJob, snapshot);
        } catch (RuntimeException exception) {
            Instant completedAt = Instant.now();
            int totalRows = parsed == null ? 0 : parsed.totalRows();
            int rejectedRows = parsed == null ? 0 : parsed.errors().size();
            importTransactions.fail(new PortfolioImportJob(
                    jobId, portfolio.id(), BrokerType.ZERODHA, originalFileName,
                    ImportStatus.FAILED, totalRows, 0, rejectedRows,
                    parsed == null ? List.of() : parsed.errors(),
                    safeMessage(exception), startedAt, completedAt, startedAt));
            if (exception instanceof ApiException apiException) {
                throw apiException;
            }
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Unable to import the XLSX holdings file: " + safeMessage(exception),
                    exception);
        }
    }

    @Transactional
    public PortfolioSummary getSummary() {
        Portfolio portfolio = repository.getOrCreatePortfolio(
                BrokerType.ZERODHA, DEFAULT_PORTFOLIO_NAME);
        List<PortfolioHoldingValuation> valuations = valueHoldings(
                repository.findHoldings(portfolio.id(), 0, Integer.MAX_VALUE).content());
        BigDecimal invested = valuations.stream()
                .map(PortfolioHoldingValuation::investedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal current = valuations.stream()
                .map(PortfolioHoldingValuation::currentValue)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPnl = current.subtract(invested);
        BigDecimal dayPnl = valuations.stream()
                .map(PortfolioHoldingValuation::dayPnl)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previousValue = current.subtract(dayPnl);
        Instant lastImportedAt = repository.findLatestSnapshot(portfolio.id())
                .map(PortfolioSnapshot::capturedAt)
                .orElse(null);
        Instant latestPriceAt = valuations.stream()
                .map(PortfolioHoldingValuation::priceCapturedAt)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
        return new PortfolioSummary(
                invested, current, totalPnl, percentage(totalPnl, invested),
                current, totalPnl, percentage(totalPnl, invested),
                dayPnl, percentage(dayPnl, previousValue),
                valuations.size(), lastImportedAt, latestPriceAt);
    }

    @Transactional
    public PageResult<PortfolioHoldingValuation> getHoldings(int page, int size) {
        Portfolio portfolio = repository.getOrCreatePortfolio(
                BrokerType.ZERODHA, DEFAULT_PORTFOLIO_NAME);
        PageResult<PortfolioHolding> holdings = repository.findHoldings(
                portfolio.id(), page, size);
        return new PageResult<>(
                valueHoldings(holdings.content()),
                holdings.page(), holdings.size(),
                holdings.totalElements(), holdings.totalPages());
    }

    @Transactional
    public List<Allocation> getSectorAllocation() {
        return allocationBy(valueHoldings(getAllHoldings()), false);
    }

    @Transactional
    public List<Allocation> getInstrumentAllocation() {
        return allocationBy(valueHoldings(getAllHoldings()), true);
    }

    @Transactional
    public PageResult<PortfolioImportJob> getImportJobs(int page, int size) {
        Portfolio portfolio = repository.getOrCreatePortfolio(
                BrokerType.ZERODHA, DEFAULT_PORTFOLIO_NAME);
        return repository.findImportJobs(portfolio.id(), page, size);
    }

    private List<PortfolioHolding> getAllHoldings() {
        Portfolio portfolio = repository.getOrCreatePortfolio(
                BrokerType.ZERODHA, DEFAULT_PORTFOLIO_NAME);
        return repository.findHoldings(portfolio.id(), 0, Integer.MAX_VALUE).content();
    }

    private List<PortfolioHoldingValuation> valueHoldings(List<PortfolioHolding> holdings) {
        Map<String, MarketPrice> prices = marketPriceReader.findLatestPrices();
        return holdings.stream()
                .map(holding -> valueHolding(holding, prices.get(holding.symbol())))
                .toList();
    }

    private PortfolioHoldingValuation valueHolding(
            PortfolioHolding holding,
            MarketPrice snapshot) {
        BigDecimal currentPrice = snapshot == null
                ? holding.lastPrice()
                : snapshot.currentPrice();
        BigDecimal previousClose = snapshot == null
                ? holding.previousClose()
                : snapshot.previousClose();
        BigDecimal invested = holding.quantity().multiply(holding.averageCost());
        BigDecimal currentValue = currentPrice == null
                ? null
                : holding.quantity().multiply(currentPrice);
        BigDecimal totalPnl = currentValue == null ? null : currentValue.subtract(invested);
        BigDecimal dayPnl = currentPrice == null || previousClose == null
                ? null
                : holding.quantity().multiply(currentPrice.subtract(previousClose));
        return new PortfolioHoldingValuation(
                holding, currentPrice, previousClose, currentValue, invested,
                totalPnl, totalPnl == null ? null : percentage(totalPnl, invested),
                dayPnl,
                dayPnl == null
                        ? null
                        : percentage(dayPnl, holding.quantity().multiply(previousClose)),
                snapshot == null ? "ZERODHA_IMPORT" : snapshot.source(),
                snapshot == null ? holding.asOf() : snapshot.capturedAt());
    }

    private List<Allocation> allocationBy(
            List<PortfolioHoldingValuation> valuations,
            boolean byInstrument) {
        BigDecimal total = valuations.stream()
                .map(this::allocationValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> grouped = valuations.stream().collect(Collectors.groupingBy(
                valuation -> byInstrument
                        ? valuation.holding().instrumentType().name()
                        : category(valuation.holding().sector()),
                Collectors.reducing(
                        BigDecimal.ZERO,
                        this::allocationValue,
                        BigDecimal::add)));

        return grouped.entrySet().stream()
                .map(entry -> new Allocation(
                        entry.getKey(),
                        entry.getValue(),
                        percentage(entry.getValue(), total)))
                .sorted(Comparator.comparing(Allocation::presentValue).reversed())
                .toList();
    }

    private BigDecimal allocationValue(PortfolioHoldingValuation valuation) {
        return valuation.currentValue() == null
                ? valuation.investedValue()
                : valuation.currentValue();
    }

    private PortfolioHolding toHolding(
            ParsedHolding holding,
            UUID portfolioId,
            UUID jobId,
            Instant now) {
        return new PortfolioHolding(
                UUID.randomUUID(), portfolioId, jobId, holding.symbol(), holding.isin(),
                holding.companyName(), holding.sector(),
                holding.instrumentType() == null ? InstrumentType.UNKNOWN : holding.instrumentType(),
                holding.quantity(), holding.averageCost(), holding.lastPrice(),
                holding.previousClose(), holding.investedValue(), holding.presentValue(),
                holding.unrealizedPnl(), holding.unrealizedPnlPercentage(),
                now, now, now);
    }

    private PortfolioSnapshot createSnapshot(
            UUID portfolioId,
            UUID jobId,
            List<PortfolioHolding> holdings,
            Instant now) {
        BigDecimal invested = sum(holdings, PortfolioHolding::investedValue);
        BigDecimal present = sum(holdings, PortfolioHolding::presentValue);
        BigDecimal pnl = present.subtract(invested);
        return new PortfolioSnapshot(
                UUID.randomUUID(), portfolioId, jobId, invested, present, pnl,
                percentage(pnl, invested), holdings.size(), now, now);
    }

    private BigDecimal sum(
            List<PortfolioHolding> holdings,
            java.util.function.Function<PortfolioHolding, BigDecimal> extractor) {
        return holdings.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(ONE_HUNDRED).divide(total, 4, RoundingMode.HALF_UP);
    }

    private String category(String value) {
        return value == null || value.isBlank() ? "Unclassified" : value.trim();
    }

    private String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
