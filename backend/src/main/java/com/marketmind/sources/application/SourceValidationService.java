package com.marketmind.sources.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.sources.application.PdfCapabilityChecker.PdfCapabilityResult;
import com.marketmind.sources.application.ReachabilityChecker.ReachabilityResult;
import com.marketmind.sources.application.RobotsTxtChecker.RobotsTxtResult;
import com.marketmind.sources.domain.CapabilityStatus;
import com.marketmind.sources.domain.SourceHealth;
import com.marketmind.sources.domain.SourceRegistry;
import com.marketmind.sources.domain.SourceStatus;
import com.marketmind.sources.domain.SourceValidationHistory;
import com.marketmind.sources.domain.ValidationStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceValidationService.class);

    private final SourceRegistryRepository repository;
    private final ReachabilityChecker reachabilityChecker;
    private final RobotsTxtChecker robotsTxtChecker;
    private final PdfCapabilityChecker pdfCapabilityChecker;
    private final Clock clock;

    public SourceValidationService(
            SourceRegistryRepository repository,
            ReachabilityChecker reachabilityChecker,
            RobotsTxtChecker robotsTxtChecker,
            PdfCapabilityChecker pdfCapabilityChecker,
            Clock clock) {
        this.repository = repository;
        this.reachabilityChecker = reachabilityChecker;
        this.robotsTxtChecker = robotsTxtChecker;
        this.pdfCapabilityChecker = pdfCapabilityChecker;
        this.clock = clock;
    }

    @Transactional
    public SourceValidationHistory validateSource(UUID sourceId) {
        SourceRegistry source = repository.findSourceById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Source not found: " + sourceId));
        Instant now = clock.instant();

        ReachabilityResult reachability = checkReachability(source);
        RobotsTxtResult robots = checkRobotsTxt(source);
        PdfCapabilityResult pdf = source.samplePdfUrl() == null
                ? new PdfCapabilityResult(
                        CapabilityStatus.UNKNOWN,
                        "Not checked because no sample PDF URL is configured.")
                : checkPdfCapability(source);

        ValidationStatus validationStatus =
                determineStatus(source, reachability, robots, pdf);
        String message = buildMessage(reachability, robots, pdf);
        SourceValidationHistory validation = repository.saveValidation(
                new SourceValidationHistory(
                        UUID.randomUUID(),
                        source.id(),
                        source.name(),
                        validationStatus,
                        reachability.reachable(),
                        reachability.httpStatus(),
                        reachability.latencyMs(),
                        robots.available(),
                        robots.httpStatus(),
                        pdf.status(),
                        message,
                        source.capabilities(),
                        now,
                        now));

        repository.saveHealth(new SourceHealth(
                UUID.randomUUID(),
                source.id(),
                healthStatus(source, validationStatus),
                reachability.reachable(),
                reachability.latencyMs(),
                message,
                now,
                reachability.httpStatus(),
                reachability.latencyMs(),
                robots.available(),
                robots.httpStatus(),
                pdf.status(),
                now,
                now));
        return validation;
    }

    private ReachabilityResult checkReachability(SourceRegistry source) {
        try {
            return reachabilityChecker.check(source.baseUrl());
        } catch (RuntimeException exception) {
            LOGGER.debug(
                    "Unexpected reachability validation failure for source {}",
                    source.id(),
                    exception);
            return new ReachabilityResult(
                    false,
                    null,
                    0,
                    "Source reachability check could not be completed.");
        }
    }

    private RobotsTxtResult checkRobotsTxt(SourceRegistry source) {
        try {
            return robotsTxtChecker.check(
                    source.robotsUrl() == null ? source.baseUrl() : source.robotsUrl());
        } catch (RuntimeException exception) {
            LOGGER.debug(
                    "Unexpected robots.txt validation failure for source {}",
                    source.id(),
                    exception);
            return new RobotsTxtResult(
                    false,
                    null,
                    "robots.txt check could not be completed.");
        }
    }

    private PdfCapabilityResult checkPdfCapability(SourceRegistry source) {
        try {
            return pdfCapabilityChecker.check(source.samplePdfUrl());
        } catch (RuntimeException exception) {
            LOGGER.debug(
                    "Unexpected PDF capability validation failure for source {}",
                    source.id(),
                    exception);
            return new PdfCapabilityResult(
                    CapabilityStatus.UNKNOWN,
                    "Sample PDF check could not be completed.");
        }
    }

    private ValidationStatus determineStatus(
            SourceRegistry source,
            ReachabilityResult reachability,
            RobotsTxtResult robots,
            PdfCapabilityResult pdf) {
        boolean pdfRequired = source.samplePdfUrl() != null;
        boolean allCriticalChecksPassed = reachability.reachable()
                && robots.available()
                && (!pdfRequired || pdf.status() == CapabilityStatus.SUPPORTED);
        if (allCriticalChecksPassed) {
            return ValidationStatus.SUCCESS;
        }

        boolean anyKeyCheckPassed = reachability.reachable()
                || robots.available()
                || (pdfRequired && pdf.status() == CapabilityStatus.SUPPORTED);
        if (anyKeyCheckPassed) {
            return ValidationStatus.WARNING;
        }
        return ValidationStatus.FAILED;
    }

    private SourceStatus healthStatus(SourceRegistry source, ValidationStatus validationStatus) {
        return switch (validationStatus) {
            case SUCCESS -> source.status() == SourceStatus.DISABLED
                    ? SourceStatus.DISABLED
                    : SourceStatus.ACTIVE;
            case WARNING -> SourceStatus.DEGRADED;
            case FAILED -> SourceStatus.INACTIVE;
        };
    }

    private String buildMessage(
            ReachabilityResult reachability,
            RobotsTxtResult robots,
            PdfCapabilityResult pdf) {
        return "Reachability: " + reachability.message()
                + " Robots.txt: " + robots.message()
                + " PDF capability: " + pdf.message();
    }
}
