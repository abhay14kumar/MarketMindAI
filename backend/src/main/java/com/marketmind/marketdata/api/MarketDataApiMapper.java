package com.marketmind.marketdata.api;

import com.marketmind.marketdata.application.MarketDataHealth;
import com.marketmind.marketdata.domain.MarketIndex;
import com.marketmind.marketdata.domain.StockPriceDaily;

import org.springframework.stereotype.Component;

@Component
public class MarketDataApiMapper {

    public MarketIndexResponse toResponse(MarketIndex index) {
        return new MarketIndexResponse(
                index.symbol(),
                index.name(),
                index.exchange().code(),
                index.lastValue(),
                index.changeValue(),
                index.changePercent(),
                index.currency(),
                index.asOf());
    }

    public StockPriceDailyResponse toResponse(StockPriceDaily price) {
        return new StockPriceDailyResponse(
                price.symbol(),
                price.exchange().code(),
                price.tradingDate(),
                price.open(),
                price.high(),
                price.low(),
                price.close(),
                price.adjustedClose(),
                price.volume(),
                price.currency(),
                price.source());
    }

    public MarketHealthResponse toResponse(MarketDataHealth health) {
        return new MarketHealthResponse(
                health.status(),
                health.provider(),
                health.mode(),
                health.asOf());
    }
}
