package com.marketmind.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.portfolio.domain.BrokerType;
import com.marketmind.portfolio.domain.InstrumentType;
import com.marketmind.portfolio.domain.Portfolio;
import com.marketmind.portfolio.domain.PortfolioHolding;
import com.marketmind.portfolio.domain.PortfolioImportJob;
import com.marketmind.portfolio.domain.PortfolioSnapshot;
import com.marketmind.portfolio.parser.ParsedHolding;
import com.marketmind.portfolio.parser.PortfolioFileParser;
import com.marketmind.portfolio.parser.PortfolioParseResult;

import org.junit.jupiter.api.Test;

class PortfolioServiceTest {

    @Test
    void shouldCreateSnapshotAndCompleteImport() {
        InMemoryPortfolioRepository repository = new InMemoryPortfolioRepository();
        PortfolioFileParser parser = input -> new PortfolioParseResult(
                1,
                List.of(new ParsedHolding(
                        "INFY", null, "Infosys", "IT", InstrumentType.EQUITY,
                        BigDecimal.TEN, BigDecimal.valueOf(100),
                        BigDecimal.valueOf(120), null,
                        BigDecimal.valueOf(1000), BigDecimal.valueOf(1200),
                        BigDecimal.valueOf(200), BigDecimal.valueOf(20))),
                List.of());
        PortfolioService service = new PortfolioService(
                repository, parser, new PortfolioImportTransactions(repository),
                java.util.Map::of);

        PortfolioImportResult result = service.importHoldings(
                "holdings.xlsx", new ByteArrayInputStream(new byte[0]));

        assertThat(result.snapshot().totalPresentValue()).isEqualByComparingTo("1200");
        assertThat(result.importJob().importedRows()).isEqualTo(1);
        assertThat(repository.holdings).hasSize(1);
        assertThat(repository.jobs).hasSize(1);
    }

    @Test
    void shouldPreferProviderPriceOverImportedPrice() {
        InMemoryPortfolioRepository repository = new InMemoryPortfolioRepository();
        Instant now = Instant.parse("2026-06-20T12:00:00Z");
        repository.holdings.add(new PortfolioHolding(
                UUID.randomUUID(), repository.portfolio.id(), UUID.randomUUID(),
                "INFY", null, "Infosys", "IT", InstrumentType.EQUITY,
                BigDecimal.TEN, new BigDecimal("100"),
                new BigDecimal("120"), new BigDecimal("118"),
                new BigDecimal("1000"), new BigDecimal("1200"),
                new BigDecimal("200"), new BigDecimal("20"),
                now, now, now));
        MarketPriceReader reader = () -> Map.of(
                "INFY",
                new MarketPrice(
                        "INFY", new BigDecimal("150"), new BigDecimal("145"),
                        "PUBLIC", now.plusSeconds(60)));
        PortfolioService service = new PortfolioService(
                repository,
                input -> new PortfolioParseResult(0, List.of(), List.of()),
                new PortfolioImportTransactions(repository),
                reader);

        PortfolioHoldingValuation valuation = service.getHoldings(0, 20).content().getFirst();

        assertThat(valuation.currentPrice()).isEqualByComparingTo("150");
        assertThat(valuation.currentValue()).isEqualByComparingTo("1500");
        assertThat(valuation.priceSource()).isEqualTo("PUBLIC");
    }

    private static final class InMemoryPortfolioRepository implements PortfolioRepository {

        private final Portfolio portfolio = new Portfolio(
                UUID.randomUUID(), "Zerodha Holdings", BrokerType.ZERODHA, "INR",
                Instant.now(), Instant.now());
        private final List<PortfolioHolding> holdings = new ArrayList<>();
        private final List<PortfolioImportJob> jobs = new ArrayList<>();
        private PortfolioSnapshot snapshot;

        @Override
        public Portfolio getOrCreatePortfolio(BrokerType brokerType, String name) {
            return portfolio;
        }

        @Override
        public PortfolioImportJob saveImportJob(PortfolioImportJob job) {
            jobs.removeIf(existing -> existing.id().equals(job.id()));
            jobs.add(job);
            return job;
        }

        @Override
        public void replaceHoldings(UUID portfolioId, List<PortfolioHolding> replacements) {
            holdings.clear();
            holdings.addAll(replacements);
        }

        @Override
        public PortfolioSnapshot saveSnapshot(PortfolioSnapshot value) {
            snapshot = value;
            return value;
        }

        @Override
        public Optional<PortfolioSnapshot> findLatestSnapshot(UUID portfolioId) {
            return Optional.ofNullable(snapshot);
        }

        @Override
        public PageResult<PortfolioHolding> findHoldings(UUID portfolioId, int page, int size) {
            return new PageResult<>(holdings, page, size, holdings.size(), holdings.isEmpty() ? 0 : 1);
        }

        @Override
        public PageResult<PortfolioImportJob> findImportJobs(UUID portfolioId, int page, int size) {
            return new PageResult<>(jobs, page, size, jobs.size(), jobs.isEmpty() ? 0 : 1);
        }
    }
}
