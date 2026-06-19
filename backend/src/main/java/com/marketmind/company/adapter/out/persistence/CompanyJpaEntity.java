package com.marketmind.company.adapter.out.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.marketmind.company.domain.model.MarketCapCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "companies")
class CompanyJpaEntity {

    @Id
    private UUID id;

    @Column(name = "isin", nullable = false, length = 12)
    private String isin;

    @Column(name = "nse_symbol", length = 32)
    private String nseSymbol;

    @Column(name = "bse_symbol", length = 32)
    private String bseSymbol;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "sector", length = 120)
    private String sector;

    @Column(name = "industry", length = 120)
    private String industry;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_cap_category", nullable = false, length = 30)
    private MarketCapCategory marketCapCategory;

    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "website", length = 500)
    private String website;

    @Column(name = "listing_date")
    private LocalDate listingDate;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CompanyJpaEntity() {
    }

    CompanyJpaEntity(
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
        this.id = id;
        this.isin = isin;
        this.nseSymbol = nseSymbol;
        this.bseSymbol = bseSymbol;
        this.companyName = companyName;
        this.sector = sector;
        this.industry = industry;
        this.marketCapCategory = marketCapCategory;
        this.country = country;
        this.currency = currency;
        this.website = website;
        this.listingDate = listingDate;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID getId() {
        return id;
    }

    String getIsin() {
        return isin;
    }

    String getNseSymbol() {
        return nseSymbol;
    }

    String getBseSymbol() {
        return bseSymbol;
    }

    String getCompanyName() {
        return companyName;
    }

    String getSector() {
        return sector;
    }

    String getIndustry() {
        return industry;
    }

    MarketCapCategory getMarketCapCategory() {
        return marketCapCategory;
    }

    String getCountry() {
        return country;
    }

    String getCurrency() {
        return currency;
    }

    String getWebsite() {
        return website;
    }

    LocalDate getListingDate() {
        return listingDate;
    }

    boolean isActive() {
        return active;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
