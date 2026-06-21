package com.marketmind.sources.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import com.marketmind.common.exception.ConflictException;
import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.sources.domain.AuthenticationType;
import com.marketmind.sources.domain.CapabilityType;
import com.marketmind.sources.domain.RefreshFrequency;
import com.marketmind.sources.domain.SourceRegistry;
import com.marketmind.sources.domain.SourceStatus;
import com.marketmind.sources.domain.SourceType;
import com.marketmind.sources.infrastructure.InMemorySourceRegistryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SourceRegistryServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-19T12:00:00Z");
    private static final UUID NSE_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000001");

    private SourceRegistryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new SourceRegistryService(
                new InMemorySourceRegistryRepository(),
                clock);
    }

    @Test
    void shouldReturnNineDefaultSources() {
        assertThat(service.getSources(0, 20).content())
                .hasSize(9)
                .extracting(SourceRegistry::code)
                .contains("NSE", "BSE", "SEBI", "RBI", "AMFI",
                        "YAHOO_FINANCE", "FINNHUB", "ALPHAVANTAGE", "W3C_TEST");
    }

    @Test
    void shouldCreateNormalizedSourceWithoutCredentials() {
        SourceRegistry created = service.createSource(command(" company_ir "));

        assertThat(created.id()).isNotNull();
        assertThat(created.code()).isEqualTo("COMPANY_IR");
        assertThat(created.authenticationType()).isEqualTo(AuthenticationType.NONE);
    }

    @Test
    void shouldProvideW3cPdfValidationFixture() {
        SourceRegistry source = service.getSources(0, 20).content().stream()
                .filter(item -> item.code().equals("W3C_TEST"))
                .findFirst()
                .orElseThrow();

        assertThat(source.organization()).isEqualTo("World Wide Web Consortium");
        assertThat(source.samplePdfUrl()).hasToString(
                "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf");
        assertThat(source.capabilities()).contains(
                CapabilityType.HTTP_REACHABILITY,
                CapabilityType.ROBOTS_TXT,
                CapabilityType.PDF_DOWNLOAD);
    }

    @Test
    void shouldRejectDuplicateCode() {
        assertThatThrownBy(() -> service.createSource(command("nse")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldDeleteSource() {
        service.deleteSource(NSE_ID);

        assertThatThrownBy(() -> service.getSource(NSE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private SourceRegistryCommand command(String code) {
        return new SourceRegistryCommand(
                code,
                "Company Investor Relations",
                "Example Company",
                "Company IR source.",
                SourceType.COMPANY_INVESTOR_RELATIONS,
                SourceStatus.ACTIVE,
                AuthenticationType.NONE,
                RefreshFrequency.DAILY,
                URI.create("https://example.com"),
                URI.create("https://example.com/robots.txt"),
                URI.create("https://example.com/investors"),
                null,
                Set.of(CapabilityType.INVESTOR_RELATIONS_DOCUMENTS),
                true,
                50,
                new java.math.BigDecimal("0.9000"));
    }
}
