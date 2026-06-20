package com.marketmind.sources.infrastructure;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.marketmind.sources.application.SourceRegistryRepository;
import com.marketmind.sources.domain.AuthenticationType;
import com.marketmind.sources.domain.CapabilityType;
import com.marketmind.sources.domain.RefreshFrequency;
import com.marketmind.sources.domain.SourceCapability;
import com.marketmind.sources.domain.SourceHealth;
import com.marketmind.sources.domain.SourceRegistry;
import com.marketmind.sources.domain.SourceStatus;
import com.marketmind.sources.domain.SourceType;
import com.marketmind.sources.domain.SourceValidationHistory;

public class InMemorySourceRegistryRepository implements SourceRegistryRepository {

    private static final Instant SEED_TIME = Instant.parse("2026-06-19T12:00:00Z");

    private final Map<UUID, SourceRegistry> sources = new ConcurrentHashMap<>();
    private final Map<UUID, SourceHealth> health = new ConcurrentHashMap<>();
    private final Map<UUID, SourceCapability> capabilities = new ConcurrentHashMap<>();
    private final Map<UUID, SourceValidationHistory> validations = new ConcurrentHashMap<>();

    public InMemorySourceRegistryRepository() {
        seed("71000000-0000-0000-0000-000000000001", "NSE", "National Stock Exchange of India",
                SourceType.EXCHANGE, AuthenticationType.SESSION, RefreshFrequency.MINUTELY,
                "https://www.nseindia.com",
                Set.of(CapabilityType.COMPANY_MASTER, CapabilityType.MARKET_PRICES,
                        CapabilityType.MARKET_INDEXES, CapabilityType.COMPANY_FILINGS,
                        CapabilityType.CORPORATE_ACTIONS));
        seed("71000000-0000-0000-0000-000000000002", "BSE", "BSE Limited",
                SourceType.EXCHANGE, AuthenticationType.NONE, RefreshFrequency.MINUTELY,
                "https://www.bseindia.com",
                Set.of(CapabilityType.COMPANY_MASTER, CapabilityType.MARKET_PRICES,
                        CapabilityType.MARKET_INDEXES, CapabilityType.COMPANY_FILINGS,
                        CapabilityType.CORPORATE_ACTIONS));
        seed("71000000-0000-0000-0000-000000000003", "SEBI",
                "Securities and Exchange Board of India", SourceType.REGULATOR,
                AuthenticationType.NONE, RefreshFrequency.DAILY, "https://www.sebi.gov.in",
                Set.of(CapabilityType.REGULATORY_FILINGS, CapabilityType.CORPORATE_ACTIONS));
        seed("71000000-0000-0000-0000-000000000004", "RBI", "Reserve Bank of India",
                SourceType.CENTRAL_BANK, AuthenticationType.NONE, RefreshFrequency.DAILY,
                "https://www.rbi.org.in", Set.of(CapabilityType.REGULATORY_FILINGS));
        seed("71000000-0000-0000-0000-000000000005", "AMFI",
                "Association of Mutual Funds in India", SourceType.MUTUAL_FUND_ASSOCIATION,
                AuthenticationType.NONE, RefreshFrequency.DAILY, "https://www.amfiindia.com",
                Set.of(CapabilityType.MUTUAL_FUND_DATA));
        seed("71000000-0000-0000-0000-000000000006", "YAHOO_FINANCE", "Yahoo Finance",
                SourceType.MARKET_DATA_PROVIDER, AuthenticationType.NONE,
                RefreshFrequency.MINUTELY, "https://finance.yahoo.com",
                Set.of(CapabilityType.MARKET_PRICES, CapabilityType.MARKET_INDEXES));
        seed("71000000-0000-0000-0000-000000000007", "FINNHUB", "Finnhub",
                SourceType.MARKET_DATA_PROVIDER, AuthenticationType.API_KEY,
                RefreshFrequency.REAL_TIME, "https://finnhub.io",
                Set.of(CapabilityType.MARKET_PRICES, CapabilityType.COMPANY_MASTER,
                        CapabilityType.FINANCIAL_STATEMENTS));
        seed("71000000-0000-0000-0000-000000000008", "ALPHAVANTAGE", "AlphaVantage",
                SourceType.MARKET_DATA_PROVIDER, AuthenticationType.API_KEY,
                RefreshFrequency.MINUTELY, "https://www.alphavantage.co",
                Set.of(CapabilityType.MARKET_PRICES, CapabilityType.COMPANY_MASTER,
                        CapabilityType.FINANCIAL_STATEMENTS));
    }

    @Override
    public List<SourceRegistry> findAllSources() {
        return sources.values().stream()
                .sorted(Comparator.comparing(SourceRegistry::name))
                .toList();
    }

    @Override
    public Optional<SourceRegistry> findSourceById(UUID id) {
        return Optional.ofNullable(sources.get(id));
    }

    @Override
    public boolean existsSourceByCode(String code, UUID excludedId) {
        return sources.values().stream()
                .anyMatch(source -> !source.id().equals(excludedId)
                        && source.code().equalsIgnoreCase(code));
    }

    @Override
    public SourceRegistry saveSource(SourceRegistry source) {
        sources.put(source.id(), source);
        return source;
    }

    @Override
    public void deleteSource(UUID id) {
        sources.remove(id);
        health.values().removeIf(item -> item.sourceId().equals(id));
        capabilities.values().removeIf(item -> item.sourceId().equals(id));
    }

    @Override
    public List<SourceHealth> findAllHealth() {
        return health.values().stream()
                .sorted(Comparator.comparing(SourceHealth::checkedAt).reversed())
                .toList();
    }

    @Override
    public SourceHealth saveHealth(SourceHealth sourceHealth) {
        health.put(sourceHealth.id(), sourceHealth);
        return sourceHealth;
    }

    @Override
    public List<SourceCapability> findAllCapabilities() {
        return capabilities.values().stream()
                .sorted(Comparator.comparing(SourceCapability::capabilityType))
                .toList();
    }

    @Override
    public void replaceCapabilities(UUID sourceId, List<SourceCapability> replacements) {
        capabilities.values().removeIf(item -> item.sourceId().equals(sourceId));
        replacements.forEach(item -> capabilities.put(item.id(), item));
    }

    @Override
    public SourceValidationHistory saveValidation(SourceValidationHistory validation) {
        validations.put(validation.id(), validation);
        return validation;
    }

    private void seed(
            String id,
            String code,
            String name,
            SourceType type,
            AuthenticationType authenticationType,
            RefreshFrequency frequency,
            String baseUrl,
            Set<CapabilityType> supportedCapabilities) {
        UUID sourceId = UUID.fromString(id);
        SourceRegistry source = new SourceRegistry(
                sourceId,
                code,
                name,
                "Default MarketMind AI source registry entry.",
                type,
                SourceStatus.ACTIVE,
                authenticationType,
                frequency,
                URI.create(baseUrl),
                URI.create(baseUrl),
                supportedCapabilities,
                true,
                SEED_TIME,
                SEED_TIME);
        sources.put(sourceId, source);
        List<SourceCapability> sourceCapabilities = new ArrayList<>();
        for (CapabilityType capability : supportedCapabilities) {
            sourceCapabilities.add(new SourceCapability(
                    UUID.nameUUIDFromBytes((code + capability).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    sourceId,
                    capability,
                    true,
                    SEED_TIME,
                    SEED_TIME));
        }
        sourceCapabilities.forEach(item -> capabilities.put(item.id(), item));
        SourceHealth sourceHealth = new SourceHealth(
                UUID.nameUUIDFromBytes((code + "-health").getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                sourceId,
                SourceStatus.ACTIVE,
                true,
                100 + Math.abs(code.hashCode() % 400),
                "Mock source is available.",
                SEED_TIME,
                SEED_TIME);
        health.put(sourceHealth.id(), sourceHealth);
    }
}
