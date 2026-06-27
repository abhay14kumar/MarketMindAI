package com.marketmind.sourceintelligence.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.marketmind.discovery.application.DiscoveryRunCommand;
import com.marketmind.discovery.application.DiscoveryService;
import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.sourceintelligence.domain.SourceActivity;
import com.marketmind.sourceintelligence.domain.SourceActivityType;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceFormat;
import com.marketmind.sourceintelligence.domain.SourceIntelligenceProfile;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;
import com.marketmind.sources.application.SourceRegistryService;
import com.marketmind.sources.application.SourceValidationService;
import com.marketmind.sources.domain.RefreshFrequency;
import com.marketmind.sources.domain.SourceHealth;
import com.marketmind.sources.domain.SourceRegistry;
import com.marketmind.sources.domain.SourceType;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceIntelligenceService {

    private static final Logger log = LoggerFactory.getLogger(SourceIntelligenceService.class);

    private final SourceRegistryService registryService;
    private final SourceValidationService validationService;
    private final SourceIntelligenceRepository repository;
    private final SourceConnectorFactory connectorFactory;
    private final SourceCapabilityDetector capabilityDetector;
    private final DiscoveryService discoveryService;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public SourceIntelligenceService(
            SourceRegistryService registryService,
            SourceValidationService validationService,
            SourceIntelligenceRepository repository,
            SourceConnectorFactory connectorFactory,
            SourceCapabilityDetector capabilityDetector,
            DiscoveryService discoveryService,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.registryService = registryService;
        this.validationService = validationService;
        this.repository = repository;
        this.connectorFactory = connectorFactory;
        this.capabilityDetector = capabilityDetector;
        this.discoveryService = discoveryService;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SourceCatalogItem> catalog() {
        List<SourceRegistry> sources = registryService.getSources(0, 100).content();
        Map<UUID, SourceHealth> latestHealth = registryService.getHealth().stream()
                .collect(Collectors.toMap(
                        SourceHealth::sourceId,
                        Function.identity(),
                        (first, second) -> first.checkedAt().isAfter(second.checkedAt())
                                ? first : second));
        return sources.stream()
                .sorted(Comparator.comparingInt(SourceRegistry::priority))
                .map(source -> toCatalogItem(
                        source,
                        repository.findProfile(source.id()).orElseGet(
                                () -> defaultProfile(source)),
                        latestHealth.get(source.id())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SourceCatalogItem getSource(UUID sourceId) {
        SourceRegistry source = registryService.getSource(sourceId);
        SourceHealth health = registryService.getHealth().stream()
                .filter(item -> item.sourceId().equals(sourceId))
                .max(Comparator.comparing(SourceHealth::checkedAt))
                .orElse(null);
        return toCatalogItem(
                source,
                repository.findProfile(sourceId).orElseGet(() -> defaultProfile(source)),
                health);
    }

    @Transactional
    public SourceCatalogItem validate(UUID sourceId) {
        SourceRegistry source = registryService.getSource(sourceId);
        var validation = validationService.validateSource(sourceId);
        SourceIntelligenceProfile profile = repository.findProfile(sourceId)
                .orElseGet(() -> defaultProfile(source));
        SourceConnector connector = selectConnector(source);
        Instant now = clock.instant();
        SourceIntelligenceProfile updated = repository.saveProfile(new SourceIntelligenceProfile(
                sourceId,
                connector.type(),
                connector.trustTier(),
                trustScore(source, connector.trustTier()),
                validation.reachable() ? freshnessScore(validation.validatedAt(), now) : 0,
                capabilityDetector.detect(source.baseUrl(), connector),
                connector.supportedDocumentTypes(),
                profile.lastCrawlAt(),
                profile.nextCrawlAt(),
                profile.schedulerState(),
                profile.totalCrawls(),
                profile.successfulCrawls(),
                profile.failedCrawls(),
                profile.documentsDiscovered(),
                profile.createdAt(),
                now));
        saveActivity(
                sourceId,
                SourceActivityType.VALIDATION,
                validation.validationStatus().name(),
                "Source validation " + validation.validationStatus().name().toLowerCase(),
                validation.message(),
                "SOURCE",
                sourceId);
        meterRegistry.counter(
                "marketmind.source.validation",
                "source", source.code(),
                "status", validation.validationStatus().name()).increment();
        log.atInfo()
                .addKeyValue("sourceId", sourceId)
                .addKeyValue("sourceCode", source.code())
                .addKeyValue("connectorType", updated.connectorType())
                .addKeyValue("validationStatus", validation.validationStatus())
                .log("Source intelligence validation completed");
        return getSource(sourceId);
    }

    @Transactional
    public SourceRefreshResult refresh(UUID sourceId) {
        SourceRegistry source = registryService.getSource(sourceId);
        SourceConnector connector = selectConnector(source);
        DiscoverySourceType discoveryType = discoveryType(source);
        Instant now = clock.instant();
        try {
            var result = discoveryService.run(new DiscoveryRunCommand(
                    discoveryType,
                    discoveryType == DiscoverySourceType.TEST_SOURCE ? null : source.baseUrl(),
                    null,
                    50)).job();
            SourceIntelligenceProfile current = repository.findProfile(sourceId)
                    .orElseGet(() -> defaultProfile(source));
            boolean failed = "FAILED".equals(result.status().name());
            repository.saveProfile(new SourceIntelligenceProfile(
                    sourceId,
                    connector.type(),
                    connector.trustTier(),
                    trustScore(source, connector.trustTier()),
                    failed ? 0 : 100,
                    capabilityDetector.detect(source.baseUrl(), connector),
                    connector.supportedDocumentTypes(),
                    now,
                    nextCrawl(source.refreshFrequency(), now),
                    source.enabled() ? "ENABLED" : "DISABLED",
                    current.totalCrawls() + 1,
                    current.successfulCrawls() + (failed ? 0 : 1),
                    current.failedCrawls() + (failed ? 1 : 0),
                    current.documentsDiscovered() + result.totalDiscovered(),
                    current.createdAt(),
                    now));
            saveActivity(
                    sourceId,
                    failed
                            ? SourceActivityType.SOURCE_FAILURE
                            : result.totalDiscovered() > 0
                                    ? SourceActivityType.NEW_FILING
                                    : SourceActivityType.REFRESH,
                    failed ? "ERROR" : result.totalDiscovered() == 0 ? "WARNING" : "SUCCESS",
                    result.totalDiscovered() > 0
                            ? "New filings discovered"
                            : "Source refresh " + (failed ? "failed" : "completed"),
                    result.message(),
                    "DISCOVERY_JOB",
                    result.id());
            meterRegistry.counter(
                    "marketmind.source.refresh",
                    "source", source.code(),
                    "connector", connector.type().name(),
                    "status", result.status().name()).increment();
            return new SourceRefreshResult(
                    sourceId,
                    result.id(),
                    result.status().name(),
                    connector.type().name(),
                    result.totalDiscovered(),
                    result.message(),
                    result.recommendation());
        } catch (RuntimeException exception) {
            saveActivity(
                    sourceId,
                    SourceActivityType.SOURCE_FAILURE,
                    "ERROR",
                    "Source refresh failed",
                    safeMessage(exception),
                    "SOURCE",
                    sourceId);
            log.atWarn()
                    .addKeyValue("sourceId", sourceId)
                    .addKeyValue("sourceCode", source.code())
                    .addKeyValue("connectorType", connector.type())
                    .setCause(exception)
                    .log("Source intelligence refresh failed");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<SourceActivity> activity(int limit) {
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Activity limit must be between 1 and 200.");
        }
        return repository.findActivity(limit);
    }

    @Transactional(readOnly = true)
    public List<SourceCoverageRow> coverage() {
        return repository.coverage();
    }

    @Transactional(readOnly = true)
    public SourceIntelligenceMetrics metrics() {
        return repository.metrics();
    }

    public List<ConnectorDescriptor> connectors() {
        return connectorFactory.connectors().stream()
                .map(connector -> new ConnectorDescriptor(
                        connector.type().name(),
                        connector.trustTier().name(),
                        connector.supportedFormats(),
                        connector.supportedDocumentTypes()))
                .sorted(Comparator.comparing(ConnectorDescriptor::connectorType))
                .toList();
    }

    public Set<SourceFormat> formats() {
        return Set.of(SourceFormat.values());
    }

    private SourceCatalogItem toCatalogItem(
            SourceRegistry source,
            SourceIntelligenceProfile profile,
            SourceHealth health) {
        return new SourceCatalogItem(
                source.id(), source.code(), source.name(), source.organization(),
                source.sourceType().name(), source.status().name(), source.baseUrl(),
                profile.trustTier() == SourceTrustTier.OFFICIAL,
                source.priority(), source.reliabilityScore(),
                profile.trustScore(), profile.freshnessScore(),
                profile.trustTier(), profile.connectorType(), source.capabilities(),
                profile.supportedFormats(), profile.supportedDocumentTypes(),
                health == null ? null : health.available(),
                health == null ? null : health.latencyMs(),
                health == null ? null : health.lastHttpStatus(),
                health == null ? null : health.lastValidatedAt(),
                profile.lastCrawlAt(), profile.nextCrawlAt(), profile.schedulerState(),
                profile.totalCrawls(), profile.successfulCrawls(),
                profile.failedCrawls(), profile.documentsDiscovered());
    }

    private SourceIntelligenceProfile defaultProfile(SourceRegistry source) {
        SourceConnector connector = selectConnector(source);
        Instant now = clock.instant();
        return new SourceIntelligenceProfile(
                source.id(), connector.type(), connector.trustTier(),
                trustScore(source, connector.trustTier()), 0,
                capabilityDetector.detect(source.baseUrl(), connector),
                connector.supportedDocumentTypes(),
                null, null, "NOT_CONFIGURED", 0, 0, 0, 0, now, now);
    }

    private SourceConnector selectConnector(SourceRegistry source) {
        return connectorFactory.select(new SourceConnector.ConnectorRequest(
                discoveryType(source), source.baseUrl(), null, 50));
    }

    private DiscoverySourceType discoveryType(SourceRegistry source) {
        return switch (source.code()) {
            case "NSE" -> DiscoverySourceType.NSE;
            case "BSE" -> DiscoverySourceType.BSE;
            case "SEBI" -> DiscoverySourceType.SEBI;
            case "RBI" -> DiscoverySourceType.RBI;
            case "TEST_SOURCE" -> DiscoverySourceType.TEST_SOURCE;
            default -> source.sourceType() == SourceType.COMPANY_INVESTOR_RELATIONS
                    ? DiscoverySourceType.COMPANY_IR
                    : DiscoverySourceType.COMPANY_IR;
        };
    }

    private int trustScore(SourceRegistry source, SourceTrustTier trustTier) {
        int reliability = source.reliabilityScore()
                .multiply(BigDecimal.valueOf(100)).intValue();
        int trustBonus = switch (trustTier) {
            case OFFICIAL -> 10;
            case AUTHORIZED -> 5;
            case THIRD_PARTY, TEST -> 0;
        };
        return Math.min(100, reliability + trustBonus);
    }

    private int freshnessScore(Instant observedAt, Instant now) {
        long hours = Math.max(0, Duration.between(observedAt, now).toHours());
        return (int) Math.max(0, 100 - Math.min(100, hours));
    }

    private Instant nextCrawl(RefreshFrequency frequency, Instant now) {
        return switch (frequency) {
            case REAL_TIME, MINUTELY -> now.plus(Duration.ofMinutes(1));
            case HOURLY -> now.plus(Duration.ofHours(1));
            case DAILY -> now.plus(Duration.ofDays(1));
            case WEEKLY -> now.plus(Duration.ofDays(7));
            case ON_DEMAND -> null;
        };
    }

    private void saveActivity(
            UUID sourceId,
            SourceActivityType type,
            String severity,
            String title,
            String message,
            String relatedType,
            UUID relatedId) {
        Instant now = clock.instant();
        repository.saveActivity(new SourceActivity(
                UUID.randomUUID(), sourceId, type, severity, title,
                message == null ? "No additional details were provided." : message,
                relatedType, relatedId, now, now));
    }

    private String safeMessage(Throwable exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    public record ConnectorDescriptor(
            String connectorType,
            String trustTier,
            Set<SourceFormat> supportedFormats,
            Set<DiscoveredDocumentType> supportedDocumentTypes) {
    }
}
