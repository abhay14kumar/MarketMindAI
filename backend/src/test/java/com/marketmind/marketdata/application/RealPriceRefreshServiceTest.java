package com.marketmind.marketdata.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.marketdata.domain.Exchange;
import com.marketmind.marketdata.domain.MarketInstrument;
import com.marketmind.marketdata.domain.PriceFeedJob;
import com.marketmind.marketdata.domain.PriceFeedStatus;
import com.marketmind.marketdata.domain.PriceSnapshot;
import com.marketmind.marketdata.domain.PriceSource;
import com.marketmind.marketdata.infrastructure.PriceProviderProperties;

import org.junit.jupiter.api.Test;

class RealPriceRefreshServiceTest {

    @Test
    void shouldContinueAfterOneProviderFailure() {
        InMemoryRepository repository = new InMemoryRepository();
        repository.inputs.add(new PortfolioPriceInput(
                "INFY", null, "Infosys", BigDecimal.ONE, null, null));
        repository.inputs.add(new PortfolioPriceInput(
                "FAIL", null, "Failure", BigDecimal.ONE, null, null));
        PriceProvider provider = new PriceProvider() {
            @Override
            public String providerName() {
                return "PUBLIC";
            }

            @Override
            public PriceProviderResult fetchQuote(String symbol) {
                if (symbol.equals("FAIL")) {
                    throw new PriceProviderException("Provider unavailable.");
                }
                return new PriceProviderResult(
                        symbol, symbol + ".NS", new BigDecimal("100"),
                        new BigDecimal("98"), "INR", PriceSource.PUBLIC,
                        Instant.parse("2026-06-20T10:00:00Z"));
            }
        };
        RealPriceRefreshService service = new RealPriceRefreshService(
                repository, provider,
                new PriceProviderProperties(
                        "PUBLIC",
                        new PriceProviderProperties.Refresh(false, 60),
                        new PriceProviderProperties.Provider(10, "https://example.invalid")),
                Clock.fixed(Instant.parse("2026-06-20T12:00:00Z"), ZoneOffset.UTC));

        PriceFeedJob job = service.refreshPortfolioPrices();

        assertThat(job.status()).isEqualTo(PriceFeedStatus.COMPLETED);
        assertThat(job.updatedInstruments()).isEqualTo(1);
        assertThat(job.failedInstruments()).isEqualTo(1);
        assertThat(job.errorMessage()).contains("FAIL");
        assertThat(repository.snapshots).hasSize(1);
    }

    private static final class InMemoryRepository implements PriceFeedRepository {

        private final List<PortfolioPriceInput> inputs = new ArrayList<>();
        private final List<MarketInstrument> instruments = new ArrayList<>();
        private final List<PriceSnapshot> snapshots = new ArrayList<>();
        private PriceFeedJob latestJob;

        @Override
        public MarketInstrument saveInstrument(MarketInstrument instrument) {
            instruments.add(instrument);
            return instrument;
        }

        @Override
        public Optional<MarketInstrument> findInstrument(String symbol, Exchange exchange) {
            return instruments.stream().filter(item -> item.symbol().equals(symbol)).findFirst();
        }

        @Override
        public List<MarketInstrument> findInstruments() {
            return List.copyOf(instruments);
        }

        @Override
        public PriceSnapshot saveSnapshot(PriceSnapshot snapshot, UUID feedJobId) {
            snapshots.add(snapshot);
            return snapshot;
        }

        @Override
        public Optional<PriceSnapshot> findLatest(String symbol) {
            return Optional.empty();
        }

        @Override
        public List<PriceSnapshot> findAllLatest() {
            return List.copyOf(snapshots);
        }

        @Override
        public PriceFeedJob saveJob(PriceFeedJob job) {
            latestJob = job;
            return job;
        }

        @Override
        public Optional<PriceFeedJob> findLatestProviderJob() {
            return Optional.ofNullable(latestJob);
        }

        @Override
        public List<PortfolioPriceInput> findPortfolioPriceInputs() {
            return List.copyOf(inputs);
        }
    }
}
