package com.marketmind.marketdata.infrastructure;

import com.marketmind.marketdata.application.PriceProvider;
import com.marketmind.marketdata.application.PriceProviderException;
import com.marketmind.marketdata.application.PriceProviderResult;

import org.springframework.stereotype.Component;

@Component
public class MockPriceProvider implements PriceProvider {

    @Override
    public String providerName() {
        return "MOCK";
    }

    @Override
    public PriceProviderResult fetchQuote(String symbol) {
        throw new PriceProviderException(
                "Mock prices are generated through the mock refresh workflow.");
    }
}
