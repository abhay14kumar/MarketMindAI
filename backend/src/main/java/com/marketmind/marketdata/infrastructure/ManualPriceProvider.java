package com.marketmind.marketdata.infrastructure;

import com.marketmind.marketdata.application.PriceProvider;
import com.marketmind.marketdata.application.PriceProviderException;
import com.marketmind.marketdata.application.PriceProviderResult;

import org.springframework.stereotype.Component;

@Component
public class ManualPriceProvider implements PriceProvider {

    @Override
    public String providerName() {
        return "MANUAL";
    }

    @Override
    public PriceProviderResult fetchQuote(String symbol) {
        throw new PriceProviderException(
                "Manual prices must be submitted through the manual price API.");
    }
}
