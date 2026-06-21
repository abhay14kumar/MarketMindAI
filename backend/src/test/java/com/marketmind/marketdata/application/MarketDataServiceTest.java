package com.marketmind.marketdata.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.marketmind.marketdata.domain.ExchangeDetails;
import com.marketmind.marketdata.domain.MarketIndex;
import com.marketmind.marketdata.domain.StockPriceDaily;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketDataServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-19T12:00:00Z");

    private StubMarketDataProvider provider;
    private MarketDataService service;

    @BeforeEach
    void setUp() {
        provider = new StubMarketDataProvider();
        service = new MarketDataService(provider, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldReturnMarketIndexesFromProvider() {
        assertThat(service.getMarketIndexes())
                .extracting(MarketIndex::symbol)
                .containsExactly("NIFTY50");
    }

    @Test
    void shouldNormalizeSymbolBeforeLoadingPrices() {
        List<StockPriceDaily> prices = service.getDailyPrices(" reliance ");

        assertThat(provider.requestedSymbol).isEqualTo("RELIANCE");
        assertThat(prices).extracting(StockPriceDaily::symbol).containsExactly("RELIANCE");
    }

    @Test
    void shouldRejectBlankSymbol() {
        assertThatThrownBy(() -> service.getDailyPrices("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Symbol");
    }

    @Test
    void shouldReportMockProviderHealth() {
        MarketDataHealth health = service.getHealth();

        assertThat(health.status()).isEqualTo("UP");
        assertThat(health.provider()).isEqualTo("TEST_MOCK");
        assertThat(health.mode()).isEqualTo("MOCK");
        assertThat(health.asOf()).isEqualTo(NOW);
    }

    private static final class StubMarketDataProvider implements MarketDataProvider {

        private final ExchangeDetails exchange = new ExchangeDetails(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                "NSE",
                "National Stock Exchange of India",
                "IN",
                "INR",
                "Asia/Kolkata",
                true,
                NOW,
                NOW);

        private String requestedSymbol;

        @Override
        public List<MarketIndex> findMarketIndexes() {
            return List.of(new MarketIndex(
                    UUID.fromString("30000000-0000-0000-0000-000000000001"),
                    exchange,
                    "NIFTY50",
                    "NIFTY 50",
                    "INR",
                    new BigDecimal("24853.40"),
                    new BigDecimal("126.15"),
                    new BigDecimal("0.51"),
                    NOW,
                    true));
        }

        @Override
        public List<StockPriceDaily> findDailyPrices(String symbol) {
            requestedSymbol = symbol;
            return List.of(new StockPriceDaily(
                    UUID.fromString("40000000-0000-0000-0000-000000000001"),
                    UUID.fromString("20000000-0000-0000-0000-000000000001"),
                    exchange,
                    symbol,
                    LocalDate.of(2026, 6, 19),
                    new BigDecimal("2930.25"),
                    new BigDecimal("2973.30"),
                    new BigDecimal("2917.40"),
                    new BigDecimal("2948.50"),
                    new BigDecimal("2948.50"),
                    7_850_000,
                    "INR",
                    providerName()));
        }

        @Override
        public String providerName() {
            return "TEST_MOCK";
        }
    }
}
