package com.marketmind.marketdata.mapper;

import com.marketmind.marketdata.domain.MarketInstrument;
import com.marketmind.marketdata.domain.PriceFeedJob;
import com.marketmind.marketdata.domain.PriceSnapshot;
import com.marketmind.marketdata.dto.MarketInstrumentResponse;
import com.marketmind.marketdata.dto.PriceFeedJobResponse;
import com.marketmind.marketdata.dto.PriceSnapshotResponse;

import org.springframework.stereotype.Component;

@Component
public class PriceFeedMapper {

    public MarketInstrumentResponse toResponse(MarketInstrument instrument) {
        return new MarketInstrumentResponse(
                instrument.id(), instrument.symbol(), instrument.isin(), instrument.name(),
                instrument.exchange(), instrument.currency(), instrument.active(),
                instrument.createdAt(), instrument.updatedAt());
    }

    public PriceSnapshotResponse toResponse(PriceSnapshot snapshot) {
        return new PriceSnapshotResponse(
                snapshot.id(), snapshot.symbol(), snapshot.exchange(),
                snapshot.lastPrice(), snapshot.previousClose(), snapshot.source(),
                snapshot.capturedAt());
    }

    public PriceFeedJobResponse toResponse(PriceFeedJob job) {
        return new PriceFeedJobResponse(
                job.id(), job.source(), job.provider(), job.status(),
                job.requestedInstruments(), job.updatedInstruments(),
                job.failedInstruments(), job.errorMessage(),
                job.startedAt(), job.completedAt());
    }
}
