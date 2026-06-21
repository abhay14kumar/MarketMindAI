package com.marketmind.marketdata.infrastructure;

import java.net.http.HttpClient;
import java.time.Duration;
import com.marketmind.marketdata.application.PriceProvider;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties(PriceProviderProperties.class)
public class PriceProviderConfiguration {

    @Bean
    HttpClient priceProviderHttpClient(PriceProviderProperties properties) {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(
                        properties.providerSettings().timeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Bean
    @Primary
    PriceProvider activePriceProvider(
            PublicQuotePriceProvider publicProvider,
            ManualPriceProvider manualProvider,
            MockPriceProvider mockProvider,
            PriceProviderProperties properties) {
        return switch (properties.provider().trim().toUpperCase(java.util.Locale.ROOT)) {
            case "PUBLIC" -> publicProvider;
            case "MANUAL" -> manualProvider;
            case "MOCK" -> mockProvider;
            default -> throw new IllegalStateException(
                    "Unsupported market.price.provider: " + properties.provider());
        };
    }
}
