package com.marketmind.marketdata.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.marketdata.domain.Exchange;
import com.marketmind.marketdata.domain.MarketInstrument;
import com.marketmind.marketdata.domain.PriceFeedJob;
import com.marketmind.marketdata.domain.PriceFeedStatus;
import com.marketmind.marketdata.domain.PriceSnapshot;
import com.marketmind.marketdata.domain.PriceSource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceFeedService {

    private final PriceFeedRepository repository;
    private final Clock clock;

    public PriceFeedService(PriceFeedRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public MarketInstrument createInstrument(
            String symbol,
            String isin,
            String name,
            Exchange exchange,
            String currency) {
        Instant now = clock.instant();
        MarketInstrument instrument = new MarketInstrument(
                UUID.randomUUID(),
                normalizeSymbol(symbol),
                blankToNull(isin),
                blankToNull(name),
                exchange,
                normalizeCurrency(currency),
                true,
                now,
                now);
        return repository.saveInstrument(instrument);
    }

    @Transactional(readOnly = true)
    public List<MarketInstrument> getInstruments() {
        return repository.findInstruments();
    }

    @Transactional
    public PriceSnapshot updateManualPrice(
            String symbol,
            Exchange exchange,
            BigDecimal lastPrice,
            BigDecimal previousClose,
            PriceSource source) {
        validatePrice(lastPrice, "lastPrice");
        validatePrice(previousClose, "previousClose");
        String normalizedSymbol = normalizeSymbol(symbol);
        MarketInstrument instrument = repository.findInstrument(normalizedSymbol, exchange)
                .orElseGet(() -> createInstrument(
                        normalizedSymbol, null, normalizedSymbol, exchange, "INR"));
        Instant now = clock.instant();
        return repository.saveSnapshot(new PriceSnapshot(
                UUID.randomUUID(), instrument.id(), instrument.symbol(),
                instrument.exchange(), lastPrice, previousClose, source, now, now), null);
    }

    @Transactional(readOnly = true)
    public PriceSnapshot getLatestPrice(String symbol) {
        String normalized = normalizeSymbol(symbol);
        return repository.findLatest(normalized)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No price snapshot exists for symbol: " + normalized));
    }

    @Transactional(readOnly = true)
    public List<PriceSnapshot> getLatestPrices() {
        return repository.findAllLatest();
    }

    @Transactional
    public PriceFeedJob refreshMockPrices() {
        Instant startedAt = clock.instant();
        List<PortfolioPriceInput> inputs = repository.findPortfolioPriceInputs();
        PriceFeedJob started = new PriceFeedJob(
                UUID.randomUUID(), PriceSource.MOCK, "MOCK", PriceFeedStatus.STARTED,
                inputs.size(), 0, 0, null, startedAt, null, startedAt);
        repository.saveJob(started);

        int updated = 0;
        try {
            for (PortfolioPriceInput input : inputs) {
                MarketInstrument instrument = repository.findInstrument(
                                input.symbol(), Exchange.NSE)
                        .orElseGet(() -> createInstrument(
                                input.symbol(), input.isin(), input.name(),
                                Exchange.NSE, "INR"));
                BigDecimal previousClose = firstNonNull(
                        input.importedPreviousClose(),
                        input.importedLastPrice(),
                        input.averageCost());
                BigDecimal movement = mockMovement(input.symbol());
                BigDecimal lastPrice = previousClose.multiply(BigDecimal.ONE.add(movement))
                        .setScale(4, RoundingMode.HALF_UP)
                        .max(BigDecimal.ZERO);
                repository.saveSnapshot(new PriceSnapshot(
                        UUID.randomUUID(), instrument.id(), instrument.symbol(),
                        instrument.exchange(), lastPrice, previousClose,
                        PriceSource.MOCK, clock.instant(), clock.instant()), started.id());
                updated++;
            }
            Instant completedAt = clock.instant();
            PriceFeedJob completed = new PriceFeedJob(
                    started.id(), PriceSource.MOCK, "MOCK", PriceFeedStatus.COMPLETED,
                    inputs.size(), updated, 0, null, startedAt, completedAt, startedAt);
            return repository.saveJob(completed);
        } catch (RuntimeException exception) {
            repository.saveJob(new PriceFeedJob(
                    started.id(), PriceSource.MOCK, "MOCK", PriceFeedStatus.FAILED,
                    inputs.size(), updated, inputs.size() - updated, safeMessage(exception),
                    startedAt, clock.instant(), startedAt));
            throw exception;
        }
    }

    private BigDecimal mockMovement(String symbol) {
        int bucket = Math.floorMod(symbol.hashCode(), 601) - 300;
        return BigDecimal.valueOf(bucket, 4);
    }

    private BigDecimal firstNonNull(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return BigDecimal.ZERO;
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol must not be blank.");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCurrency(String currency) {
        String value = currency == null || currency.isBlank() ? "INR" : currency;
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private void validatePrice(BigDecimal price, String field) {
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException(field + " must be zero or greater.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safeMessage(Throwable exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
