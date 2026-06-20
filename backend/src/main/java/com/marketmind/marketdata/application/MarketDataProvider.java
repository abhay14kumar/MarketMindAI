package com.marketmind.marketdata.application;

import java.util.List;

import com.marketmind.marketdata.domain.MarketIndex;
import com.marketmind.marketdata.domain.StockPriceDaily;

public interface MarketDataProvider {

    List<MarketIndex> findMarketIndexes();

    List<StockPriceDaily> findDailyPrices(String symbol);

    String providerName();
}
