package com.marketmind.marketdata.infrastructure;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.marketdata.application.PriceProvider;
import com.marketmind.marketdata.application.PriceProviderException;
import com.marketmind.marketdata.application.PriceProviderResult;
import com.marketmind.marketdata.application.SymbolMapper;
import com.marketmind.marketdata.domain.Exchange;
import com.marketmind.marketdata.domain.PriceSource;

import org.springframework.stereotype.Component;

@Component
public class PublicQuotePriceProvider implements PriceProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SymbolMapper symbolMapper;
    private final PriceProviderProperties properties;

    public PublicQuotePriceProvider(
            HttpClient priceProviderHttpClient,
            ObjectMapper objectMapper,
            SymbolMapper symbolMapper,
            PriceProviderProperties properties) {
        this.httpClient = priceProviderHttpClient;
        this.objectMapper = objectMapper;
        this.symbolMapper = symbolMapper;
        this.properties = properties;
    }

    @Override
    public String providerName() {
        return "PUBLIC";
    }

    @Override
    public PriceProviderResult fetchQuote(String symbol) {
        String providerSymbol = symbolMapper.toProviderSymbol(symbol, Exchange.NSE);
        URI uri = URI.create(properties.providerSettings().baseUrl()
                + "/v8/finance/chart/"
                + URLEncoder.encode(providerSymbol, StandardCharsets.UTF_8)
                + "?interval=1d&range=5d");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(properties.providerSettings().timeoutSeconds()))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 MarketMindAI-PublicQuote/1.0")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PriceProviderException(
                        "Public quote provider returned HTTP " + response.statusCode() + ".");
            }
            return parse(symbol, providerSymbol, response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PriceProviderException("Public quote request was interrupted.", exception);
        } catch (IOException exception) {
            throw new PriceProviderException("Public quote provider is unavailable.", exception);
        }
    }

    PriceProviderResult parse(String symbol, String providerSymbol, String body) {
        try {
            JsonNode result = objectMapper.readTree(body)
                    .path("chart").path("result").path(0);
            JsonNode meta = result.path("meta");
            if (meta.isMissingNode() || meta.isNull()) {
                throw new PriceProviderException("Public quote response has no quote result.");
            }
            BigDecimal lastPrice = decimal(meta, "regularMarketPrice");
            BigDecimal previousClose = decimal(meta, "chartPreviousClose");
            if (previousClose == null) {
                previousClose = decimal(meta, "previousClose");
            }
            if (lastPrice == null || previousClose == null) {
                throw new PriceProviderException(
                        "Public quote response is missing price fields.");
            }
            long epochSeconds = meta.path("regularMarketTime").asLong(0);
            return new PriceProviderResult(
                    symbol, providerSymbol, lastPrice, previousClose,
                    meta.path("currency").asText("INR"), PriceSource.PUBLIC,
                    epochSeconds > 0 ? Instant.ofEpochSecond(epochSeconds) : Instant.now());
        } catch (PriceProviderException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PriceProviderException("Public quote response is invalid.", exception);
        }
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.decimalValue() : null;
    }
}
