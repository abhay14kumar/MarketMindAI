package com.marketmind.marketdata.application;

public interface PriceProvider {

    String providerName();

    PriceProviderResult fetchQuote(String symbol);
}
