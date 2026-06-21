package com.marketmind.marketdata.infrastructure;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.marketmind.marketdata.application.PriceFeedRepository;
import com.marketmind.portfolio.application.MarketPrice;
import com.marketmind.portfolio.application.MarketPriceReader;

import org.springframework.stereotype.Component;

@Component
public class PortfolioMarketPriceAdapter implements MarketPriceReader {

    private final PriceFeedRepository repository;

    public PortfolioMarketPriceAdapter(PriceFeedRepository repository) {
        this.repository = repository;
    }

    @Override
    public Map<String, MarketPrice> findLatestPrices() {
        return repository.findAllLatest().stream()
                .map(snapshot -> new MarketPrice(
                        snapshot.symbol(), snapshot.lastPrice(), snapshot.previousClose(),
                        snapshot.source().name(), snapshot.capturedAt()))
                .collect(Collectors.toUnmodifiableMap(
                        MarketPrice::symbol,
                        Function.identity(),
                        (first, second) -> first));
    }
}
