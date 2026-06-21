package com.marketmind.sources.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

import com.marketmind.sources.domain.CapabilityStatus;
import com.marketmind.sources.domain.ValidationStatus;
import com.marketmind.sources.infrastructure.InMemorySourceRegistryRepository;

import org.junit.jupiter.api.Test;

class SourceValidationServiceTest {

    private static final UUID NSE_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-06-20T12:00:00Z");

    @Test
    void shouldPersistSuccessfulRealValidationResult() {
        InMemorySourceRegistryRepository repository = new InMemorySourceRegistryRepository();
        SourceValidationService service = new SourceValidationService(
                repository,
                sourceUrl -> new ReachabilityChecker.ReachabilityResult(
                        true, 200, 123, "Reachable."),
                sourceUrl -> new RobotsTxtChecker.RobotsTxtResult(
                        true, 200, "robots.txt available."),
                samplePdfUrl -> new PdfCapabilityChecker.PdfCapabilityResult(
                        CapabilityStatus.UNKNOWN, "No sample PDF."),
                Clock.fixed(NOW, ZoneOffset.UTC));

        var result = service.validateSource(NSE_ID);

        assertThat(result.sourceName()).isEqualTo("National Stock Exchange of India");
        assertThat(result.validationStatus()).isEqualTo(ValidationStatus.SUCCESS);
        assertThat(result.reachable()).isTrue();
        assertThat(result.httpStatus()).isEqualTo(200);
        assertThat(repository.findAllHealth().getFirst().lastValidatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRecordFailureWithoutThrowingWhenSourceIsUnavailable() {
        InMemorySourceRegistryRepository repository = new InMemorySourceRegistryRepository();
        AtomicBoolean robotsChecked = new AtomicBoolean();
        AtomicBoolean pdfChecked = new AtomicBoolean();
        SourceValidationService service = new SourceValidationService(
                repository,
                sourceUrl -> new ReachabilityChecker.ReachabilityResult(
                        false, null, 15_000, "Timed out."),
                sourceUrl -> {
                    robotsChecked.set(true);
                    return new RobotsTxtChecker.RobotsTxtResult(
                            false, null, "robots.txt unavailable.");
                },
                sourceUrl -> {
                    pdfChecked.set(true);
                    return new PdfCapabilityChecker.PdfCapabilityResult(
                            CapabilityStatus.UNKNOWN, "PDF unavailable.");
                },
                Clock.fixed(NOW, ZoneOffset.UTC));

        var result = service.validateSource(NSE_ID);

        assertThat(result.validationStatus()).isEqualTo(ValidationStatus.FAILED);
        assertThat(result.reachable()).isFalse();
        assertThat(result.pdfCapabilityStatus()).isEqualTo(CapabilityStatus.UNKNOWN);
        assertThat(robotsChecked).isTrue();
        assertThat(pdfChecked).isFalse();
        assertThat(result.message())
                .contains("Reachability:", "Robots.txt:", "PDF capability:");
    }

    @Test
    void shouldReturnWarningWhenReachabilityFailsButRobotsTxtWorks() {
        InMemorySourceRegistryRepository repository = new InMemorySourceRegistryRepository();
        SourceValidationService service = new SourceValidationService(
                repository,
                sourceUrl -> new ReachabilityChecker.ReachabilityResult(
                        false, null, 200, "Base URL unavailable."),
                sourceUrl -> new RobotsTxtChecker.RobotsTxtResult(
                        true, 200, "robots.txt available."),
                sourceUrl -> {
                    throw new AssertionError("PDF check should be skipped without a sample URL");
                },
                Clock.fixed(NOW, ZoneOffset.UTC));

        var result = service.validateSource(NSE_ID);

        assertThat(result.validationStatus()).isEqualTo(ValidationStatus.WARNING);
        assertThat(result.robotsTxtAvailable()).isTrue();
    }

    @Test
    void shouldCheckPdfEvenWhenReachabilityFailsIfSampleUrlExists() {
        InMemorySourceRegistryRepository repository = new InMemorySourceRegistryRepository();
        var source = repository.findSourceById(NSE_ID).orElseThrow();
        repository.saveSource(new com.marketmind.sources.domain.SourceRegistry(
                source.id(),
                source.code(),
                source.name(),
                source.organization(),
                source.description(),
                source.sourceType(),
                source.status(),
                source.authenticationType(),
                source.refreshFrequency(),
                source.baseUrl(),
                source.robotsUrl(),
                source.documentationUrl(),
                URI.create("https://example.com/sample.pdf"),
                source.capabilities(),
                source.enabled(),
                source.priority(),
                source.reliabilityScore(),
                source.createdAt(),
                source.updatedAt()));
        AtomicBoolean pdfChecked = new AtomicBoolean();
        SourceValidationService service = new SourceValidationService(
                repository,
                sourceUrl -> new ReachabilityChecker.ReachabilityResult(
                        false, null, 200, "Base URL unavailable."),
                sourceUrl -> new RobotsTxtChecker.RobotsTxtResult(
                        false, 404, "robots.txt unavailable."),
                sourceUrl -> {
                    pdfChecked.set(true);
                    return new PdfCapabilityChecker.PdfCapabilityResult(
                            CapabilityStatus.SUPPORTED, "PDF supported.");
                },
                Clock.fixed(NOW, ZoneOffset.UTC));

        var result = service.validateSource(NSE_ID);

        assertThat(pdfChecked).isTrue();
        assertThat(result.pdfCapabilityStatus()).isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(result.validationStatus()).isEqualTo(ValidationStatus.WARNING);
    }

    @Test
    void shouldContinueOtherChecksAfterUnexpectedReachabilityError() {
        InMemorySourceRegistryRepository repository = new InMemorySourceRegistryRepository();
        AtomicBoolean robotsChecked = new AtomicBoolean();
        SourceValidationService service = new SourceValidationService(
                repository,
                sourceUrl -> {
                    throw new IllegalStateException("Stream 1 cancelled");
                },
                sourceUrl -> {
                    robotsChecked.set(true);
                    return new RobotsTxtChecker.RobotsTxtResult(
                            true, 200, "robots.txt available.");
                },
                sourceUrl -> {
                    throw new AssertionError("PDF check should be skipped without a sample URL");
                },
                Clock.fixed(NOW, ZoneOffset.UTC));

        var result = service.validateSource(NSE_ID);

        assertThat(robotsChecked).isTrue();
        assertThat(result.validationStatus()).isEqualTo(ValidationStatus.WARNING);
        assertThat(result.message()).doesNotContain("Stream 1 cancelled");
    }
}
