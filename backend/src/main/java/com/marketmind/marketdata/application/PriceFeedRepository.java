package com.marketmind.marketdata.application;

import java.util.List;
import java.util.Optional;

import com.marketmind.marketdata.domain.Exchange;
import com.marketmind.marketdata.domain.MarketInstrument;
import com.marketmind.marketdata.domain.PriceFeedJob;
import com.marketmind.marketdata.domain.PriceSnapshot;

public interface PriceFeedRepository {

    MarketInstrument saveInstrument(MarketInstrument instrument);

    Optional<MarketInstrument> findInstrument(String symbol, Exchange exchange);

    List<MarketInstrument> findInstruments();

    PriceSnapshot saveSnapshot(PriceSnapshot snapshot, java.util.UUID feedJobId);

    Optional<PriceSnapshot> findLatest(String symbol);

    List<PriceSnapshot> findAllLatest();

    PriceFeedJob saveJob(PriceFeedJob job);

    Optional<PriceFeedJob> findLatestProviderJob();

    List<PortfolioPriceInput> findPortfolioPriceInputs();
}
