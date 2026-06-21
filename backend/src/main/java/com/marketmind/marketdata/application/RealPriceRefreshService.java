package com.marketmind.marketdata.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.marketmind.marketdata.domain.Exchange;
import com.marketmind.marketdata.domain.MarketInstrument;
import com.marketmind.marketdata.domain.PriceFeedJob;
import com.marketmind.marketdata.domain.PriceFeedStatus;
import com.marketmind.marketdata.domain.PriceSnapshot;
import com.marketmind.marketdata.infrastructure.PriceProviderProperties;

import org.springframework.stereotype.Service;

@Service
public class RealPriceRefreshService {

    private static final int MAX_ERROR_SUMMARY_LENGTH = 4000;

    private final PriceFeedRepository repository;
    private final PriceProvider provider;
    private final PriceProviderProperties properties;
    private final Clock clock;

    public RealPriceRefreshService(
            PriceFeedRepository repository,
            PriceProvider activePriceProvider,
            PriceProviderProperties properties,
            Clock clock) {
        this.repository = repository;
        this.provider = activePriceProvider;
        this.properties = properties;
        this.clock = clock;
    }

    public PriceFeedJob refreshPortfolioPrices() {
        List<PortfolioPriceInput> inputs = repository.findPortfolioPriceInputs();
        Instant startedAt = clock.instant();
        UUID jobId = UUID.randomUUID();
        repository.saveJob(new PriceFeedJob(
                jobId, com.marketmind.marketdata.domain.PriceSource.PUBLIC,
                provider.providerName(), PriceFeedStatus.STARTED,
                inputs.size(), 0, 0, null, startedAt, null, startedAt));

        int successful = 0;
        List<String> errors = new ArrayList<>();
        for (PortfolioPriceInput input : inputs) {
            try {
                PriceProviderResult quote = provider.fetchQuote(input.symbol());
                MarketInstrument instrument = repository.findInstrument(
                                input.symbol(), Exchange.NSE)
                        .orElseGet(() -> saveInstrument(input, quote));
                Instant capturedAt = quote.providerTimestamp() == null
                        ? clock.instant()
                        : quote.providerTimestamp();
                repository.saveSnapshot(new PriceSnapshot(
                        UUID.randomUUID(), instrument.id(), instrument.symbol(),
                        instrument.exchange(), quote.lastPrice(), quote.previousClose(),
                        quote.source(), capturedAt, clock.instant()), jobId);
                successful++;
            } catch (RuntimeException exception) {
                errors.add(input.symbol() + ": " + safeMessage(exception));
            }
        }

        int failed = inputs.size() - successful;
        PriceFeedStatus status = successful == 0 && !inputs.isEmpty()
                ? PriceFeedStatus.FAILED
                : PriceFeedStatus.COMPLETED;
        String summary = truncate(String.join("; ", errors));
        return repository.saveJob(new PriceFeedJob(
                jobId, com.marketmind.marketdata.domain.PriceSource.PUBLIC,
                provider.providerName(), status, inputs.size(), successful, failed,
                summary, startedAt, clock.instant(), startedAt));
    }

    public PriceProviderStatus getStatus() {
        return repository.findLatestProviderJob()
                .map(job -> new PriceProviderStatus(
                        provider.providerName(), properties.refresh().enabled(),
                        properties.refresh().intervalSeconds(), job.status(),
                        job.requestedInstruments(), job.updatedInstruments(),
                        job.failedInstruments(), job.errorMessage(),
                        job.completedAt() == null ? job.startedAt() : job.completedAt()))
                .orElseGet(() -> new PriceProviderStatus(
                        provider.providerName(), properties.refresh().enabled(),
                        properties.refresh().intervalSeconds(), null,
                        0, 0, 0, null, null));
    }

    private MarketInstrument saveInstrument(
            PortfolioPriceInput input,
            PriceProviderResult quote) {
        Instant now = clock.instant();
        return repository.saveInstrument(new MarketInstrument(
                UUID.randomUUID(), input.symbol(), input.isin(), input.name(),
                Exchange.NSE, quote.currency() == null ? "INR" : quote.currency(),
                true, now, now));
    }

    private String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }

    private String truncate(String value) {
        return value.length() <= MAX_ERROR_SUMMARY_LENGTH
                ? value
                : value.substring(0, MAX_ERROR_SUMMARY_LENGTH);
    }
}
