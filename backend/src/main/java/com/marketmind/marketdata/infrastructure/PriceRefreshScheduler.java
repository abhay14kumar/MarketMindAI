package com.marketmind.marketdata.infrastructure;

import com.marketmind.marketdata.application.RealPriceRefreshService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "market.price.refresh.enabled",
        havingValue = "true")
public class PriceRefreshScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceRefreshScheduler.class);

    private final RealPriceRefreshService refreshService;

    public PriceRefreshScheduler(RealPriceRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @Scheduled(
            fixedDelayString = "#{${market.price.refresh.interval-seconds:60} * 1000}",
            initialDelayString = "#{${market.price.refresh.interval-seconds:60} * 1000}")
    public void refresh() {
        try {
            refreshService.refreshPortfolioPrices();
        } catch (RuntimeException exception) {
            LOGGER.warn("Scheduled public price refresh failed safely: {}", exception.getMessage());
        }
    }
}
