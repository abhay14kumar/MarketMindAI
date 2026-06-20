package com.marketmind.marketdata.application;

import java.time.Clock;
import java.util.List;
import java.util.Locale;

import com.marketmind.marketdata.domain.MarketIndex;
import com.marketmind.marketdata.domain.StockPriceDaily;

import org.springframework.stereotype.Service;

@Service
public class MarketDataService {

    private final MarketDataProvider marketDataProvider;
    private final Clock clock;

    public MarketDataService(MarketDataProvider marketDataProvider, Clock clock) {
        this.marketDataProvider = marketDataProvider;
        this.clock = clock;
    }

    public List<MarketIndex> getMarketIndexes() {
        return marketDataProvider.findMarketIndexes();
    }

    public List<StockPriceDaily> getDailyPrices(String symbol) {
        return marketDataProvider.findDailyPrices(normalizeSymbol(symbol));
    }

    public MarketDataHealth getHealth() {
        return new MarketDataHealth(
                "UP",
                marketDataProvider.providerName(),
                "MOCK",
                clock.instant());
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol must not be blank.");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
