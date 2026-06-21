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
import com.marketmind.marketdata.domain.PriceSnapshot;
import com.marketmind.marketdata.domain.PriceSource;

import org.junit.jupiter.api.Test;

class PriceFeedServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-20T12:00:00Z");

    @Test
    void shouldCreateInstrumentAndManualPriceSnapshot() {
        InMemoryPriceFeedRepository repository = new InMemoryPriceFeedRepository();
        PriceFeedService service = new PriceFeedService(
                repository, Clock.fixed(NOW, ZoneOffset.UTC));

        PriceSnapshot snapshot = service.updateManualPrice(
                " infy ", Exchange.NSE,
                new BigDecimal("1600"), new BigDecimal("1580"),
                PriceSource.MANUAL);

        assertThat(snapshot.symbol()).isEqualTo("INFY");
        assertThat(snapshot.lastPrice()).isEqualByComparingTo("1600");
        assertThat(service.getLatestPrice("infy")).isEqualTo(snapshot);
    }

    @Test
    void shouldCreateMockPricesForPortfolioHoldings() {
        InMemoryPriceFeedRepository repository = new InMemoryPriceFeedRepository();
        repository.inputs.add(new PortfolioPriceInput(
                "RELIANCE", null, "Reliance Industries",
                new BigDecimal("2900"), null, new BigDecimal("3000")));
        PriceFeedService service = new PriceFeedService(
                repository, Clock.fixed(NOW, ZoneOffset.UTC));

        PriceFeedJob job = service.refreshMockPrices();

        assertThat(job.updatedInstruments()).isEqualTo(1);
        assertThat(repository.snapshots).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.source()).isEqualTo(PriceSource.MOCK);
            assertThat(snapshot.previousClose()).isEqualByComparingTo("3000");
        });
    }

    private static final class InMemoryPriceFeedRepository implements PriceFeedRepository {

        private final List<MarketInstrument> instruments = new ArrayList<>();
        private final List<PriceSnapshot> snapshots = new ArrayList<>();
        private final List<PortfolioPriceInput> inputs = new ArrayList<>();

        @Override
        public MarketInstrument saveInstrument(MarketInstrument instrument) {
            instruments.removeIf(existing -> existing.symbol().equals(instrument.symbol())
                    && existing.exchange() == instrument.exchange());
            instruments.add(instrument);
            return instrument;
        }

        @Override
        public Optional<MarketInstrument> findInstrument(String symbol, Exchange exchange) {
            return instruments.stream()
                    .filter(item -> item.symbol().equals(symbol) && item.exchange() == exchange)
                    .findFirst();
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
            return snapshots.stream()
                    .filter(snapshot -> snapshot.symbol().equals(symbol))
                    .reduce((first, second) -> second);
        }

        @Override
        public List<PriceSnapshot> findAllLatest() {
            return List.copyOf(snapshots);
        }

        @Override
        public PriceFeedJob saveJob(PriceFeedJob job) {
            return job;
        }

        @Override
        public Optional<PriceFeedJob> findLatestProviderJob() {
            return Optional.empty();
        }

        @Override
        public List<PortfolioPriceInput> findPortfolioPriceInputs() {
            return List.copyOf(inputs);
        }
    }
}
