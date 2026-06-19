package com.marketmind.company.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Company(
        UUID id,
        String isin,
        String nseSymbol,
        String bseSymbol,
        String companyName,
        String sector,
        String industry,
        MarketCapCategory marketCapCategory,
        String country,
        String currency,
        String website,
        LocalDate listingDate,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
