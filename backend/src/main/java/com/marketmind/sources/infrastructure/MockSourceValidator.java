package com.marketmind.sources.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.marketmind.sources.application.SourceValidator;
import com.marketmind.sources.domain.SourceRegistry;
import com.marketmind.sources.domain.SourceValidationHistory;
import com.marketmind.sources.domain.ValidationStatus;

import org.springframework.stereotype.Component;

@Component
public class MockSourceValidator implements SourceValidator {

    private final Clock clock;

    public MockSourceValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public SourceValidationHistory validate(SourceRegistry source) {
        Instant now = clock.instant();
        boolean available = source.enabled();
        return new SourceValidationHistory(
                UUID.randomUUID(),
                source.id(),
                available ? ValidationStatus.SUCCESS : ValidationStatus.WARNING,
                available,
                75 + Math.abs(source.code().hashCode() % 650),
                available
                        ? "Mock validation completed successfully."
                        : "Source is disabled; mock validation did not test connectivity.",
                source.capabilities(),
                now,
                now);
    }
}
