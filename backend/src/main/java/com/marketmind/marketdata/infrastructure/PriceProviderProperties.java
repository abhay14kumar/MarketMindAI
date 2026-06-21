package com.marketmind.marketdata.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market.price")
public record PriceProviderProperties(
        String provider,
        Refresh refresh,
        Provider providerSettings) {

    public PriceProviderProperties {
        provider = provider == null || provider.isBlank() ? "PUBLIC" : provider;
        refresh = refresh == null ? new Refresh(false, 60) : refresh;
        providerSettings = providerSettings == null
                ? new Provider(10, "https://query2.finance.yahoo.com")
                : providerSettings;
    }

    public record Refresh(boolean enabled, long intervalSeconds) {
    }

    public record Provider(int timeoutSeconds, String baseUrl) {
    }
}
