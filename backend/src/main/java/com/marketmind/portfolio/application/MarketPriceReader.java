package com.marketmind.portfolio.application;

import java.util.Map;

public interface MarketPriceReader {

    Map<String, MarketPrice> findLatestPrices();
}
