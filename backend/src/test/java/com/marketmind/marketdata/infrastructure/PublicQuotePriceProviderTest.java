package com.marketmind.marketdata.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.http.HttpClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.marketdata.application.PriceProviderException;
import com.marketmind.marketdata.application.PriceProviderResult;
import com.marketmind.marketdata.application.SymbolMapper;
import com.marketmind.marketdata.domain.PriceSource;

import org.junit.jupiter.api.Test;

class PublicQuotePriceProviderTest {

    private final PublicQuotePriceProvider provider = new PublicQuotePriceProvider(
            HttpClient.newHttpClient(),
            new ObjectMapper(),
            new SymbolMapper(),
            new PriceProviderProperties(
                    "PUBLIC",
                    new PriceProviderProperties.Refresh(false, 60),
                    new PriceProviderProperties.Provider(10, "https://example.invalid")));

    @Test
    void shouldParseYahooChartMetadata() {
        String body = """
                {
                  "chart": {
                    "result": [{
                      "meta": {
                        "currency": "INR",
                        "regularMarketPrice": 1051.4,
                        "chartPreviousClose": 1134.9,
                        "regularMarketTime": 1781863200
                      }
                    }]
                  }
                }
                """;

        PriceProviderResult result = provider.parse("INFY", "INFY.NS", body);

        assertThat(result.lastPrice()).isEqualByComparingTo("1051.4");
        assertThat(result.previousClose()).isEqualByComparingTo("1134.9");
        assertThat(result.currency()).isEqualTo("INR");
        assertThat(result.source()).isEqualTo(PriceSource.PUBLIC);
    }

    @Test
    void shouldRejectResponseWithoutPriceFields() {
        assertThatThrownBy(() -> provider.parse(
                "INFY", "INFY.NS", "{\"chart\":{\"result\":[{\"meta\":{}}]}}"))
                .isInstanceOf(PriceProviderException.class)
                .hasMessageContaining("missing price fields");
    }
}
