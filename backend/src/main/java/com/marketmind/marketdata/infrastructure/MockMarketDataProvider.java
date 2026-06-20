package com.marketmind.marketdata.infrastructure;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.marketmind.marketdata.application.MarketDataProvider;
import com.marketmind.marketdata.domain.Exchange;
import com.marketmind.marketdata.domain.MarketIndex;
import com.marketmind.marketdata.domain.StockPriceDaily;

import org.springframework.stereotype.Component;

@Component
public class MockMarketDataProvider implements MarketDataProvider {

    private static final UUID NSE_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID BSE_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID MOCK_COMPANY_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final Instant REFERENCE_INSTANT = Instant.parse("2026-06-19T10:00:00Z");

    private final Clock clock;

    public MockMarketDataProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<MarketIndex> findMarketIndexes() {
        Exchange nse = nse();
        Exchange bse = bse();
        Instant asOf = clock.instant();
        return List.of(
                new MarketIndex(
                        UUID.fromString("30000000-0000-0000-0000-000000000001"),
                        nse,
                        "NIFTY50",
                        "NIFTY 50",
                        "INR",
                        new BigDecimal("24853.40"),
                        new BigDecimal("126.15"),
                        new BigDecimal("0.51"),
                        asOf,
                        true),
                new MarketIndex(
                        UUID.fromString("30000000-0000-0000-0000-000000000002"),
                        bse,
                        "SENSEX",
                        "S&P BSE SENSEX",
                        "INR",
                        new BigDecimal("81583.30"),
                        new BigDecimal("317.93"),
                        new BigDecimal("0.39"),
                        asOf,
                        true));
    }

    @Override
    public List<StockPriceDaily> findDailyPrices(String symbol) {
        LocalDate latestTradingDate = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        return IntStream.range(0, 5)
                .mapToObj(offset -> price(symbol, latestTradingDate.minusDays(offset), offset))
                .toList();
    }

    @Override
    public String providerName() {
        return "MARKETMIND_MOCK";
    }

    private StockPriceDaily price(String symbol, LocalDate date, int offset) {
        BigDecimal close = new BigDecimal("2948.50").subtract(BigDecimal.valueOf(offset * 11L));
        return new StockPriceDaily(
                UUID.nameUUIDFromBytes((symbol + date).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                MOCK_COMPANY_ID,
                nse(),
                symbol,
                date,
                close.subtract(new BigDecimal("18.25")),
                close.add(new BigDecimal("24.80")),
                close.subtract(new BigDecimal("31.10")),
                close,
                close,
                7_850_000L - offset * 125_000L,
                "INR",
                providerName());
    }

    private Exchange nse() {
        return exchange(NSE_ID, "NSE", "National Stock Exchange of India");
    }

    private Exchange bse() {
        return exchange(BSE_ID, "BSE", "BSE Limited");
    }

    private Exchange exchange(UUID id, String code, String name) {
        return new Exchange(
                id,
                code,
                name,
                "IN",
                "INR",
                "Asia/Kolkata",
                true,
                REFERENCE_INSTANT,
                REFERENCE_INSTANT);
    }
}
