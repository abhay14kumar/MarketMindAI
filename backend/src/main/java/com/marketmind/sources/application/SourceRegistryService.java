package com.marketmind.sources.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.marketmind.common.exception.ConflictException;
import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.sources.domain.SourceCapability;
import com.marketmind.sources.domain.SourceHealth;
import com.marketmind.sources.domain.SourceRegistry;
import com.marketmind.sources.domain.SourceValidationHistory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceRegistryService {

    private final SourceRegistryRepository repository;
    private final SourceValidator sourceValidator;
    private final Clock clock;

    public SourceRegistryService(
            SourceRegistryRepository repository,
            SourceValidator sourceValidator,
            Clock clock) {
        this.repository = repository;
        this.sourceValidator = sourceValidator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResult<SourceRegistry> getSources(int page, int size) {
        return page(repository.findAllSources(), page, size);
    }

    @Transactional(readOnly = true)
    public SourceRegistry getSource(UUID id) {
        return repository.findSourceById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Source not found: " + id));
    }

    @Transactional
    public SourceRegistry createSource(SourceRegistryCommand command) {
        String code = normalizeCode(command.code());
        ensureUniqueCode(code, null);
        Instant now = clock.instant();
        SourceRegistry source = repository.saveSource(new SourceRegistry(
                UUID.randomUUID(),
                code,
                command.name().trim(),
                trimToNull(command.description()),
                command.sourceType(),
                command.status(),
                command.authenticationType(),
                command.refreshFrequency(),
                command.baseUrl(),
                command.documentationUrl(),
                command.capabilities(),
                command.enabled(),
                now,
                now));
        replaceCapabilities(source, now);
        return source;
    }

    @Transactional
    public SourceRegistry updateSource(UUID id, SourceRegistryCommand command) {
        SourceRegistry existing = getSource(id);
        String code = normalizeCode(command.code());
        ensureUniqueCode(code, id);
        SourceRegistry updated = repository.saveSource(new SourceRegistry(
                existing.id(),
                code,
                command.name().trim(),
                trimToNull(command.description()),
                command.sourceType(),
                command.status(),
                command.authenticationType(),
                command.refreshFrequency(),
                command.baseUrl(),
                command.documentationUrl(),
                command.capabilities(),
                command.enabled(),
                existing.createdAt(),
                clock.instant()));
        replaceCapabilities(updated, clock.instant());
        return updated;
    }

    @Transactional
    public void deleteSource(UUID id) {
        getSource(id);
        repository.deleteSource(id);
    }

    @Transactional
    public SourceValidationHistory validateSource(UUID id) {
        SourceRegistry source = getSource(id);
        SourceValidationHistory validation = repository.saveValidation(
                sourceValidator.validate(source));
        repository.saveHealth(new SourceHealth(
                UUID.randomUUID(),
                source.id(),
                validation.available() ? source.status() : com.marketmind.sources.domain.SourceStatus.DEGRADED,
                validation.available(),
                validation.latencyMs(),
                validation.message(),
                validation.validatedAt(),
                clock.instant()));
        return validation;
    }

    @Transactional(readOnly = true)
    public List<SourceHealth> getHealth() {
        return repository.findAllHealth();
    }

    @Transactional(readOnly = true)
    public List<SourceCapability> getCapabilities() {
        return repository.findAllCapabilities();
    }

    private void replaceCapabilities(SourceRegistry source, Instant verifiedAt) {
        List<SourceCapability> capabilities = source.capabilities().stream()
                .map(capability -> new SourceCapability(
                        UUID.randomUUID(),
                        source.id(),
                        capability,
                        true,
                        verifiedAt,
                        verifiedAt))
                .toList();
        repository.replaceCapabilities(source.id(), capabilities);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void ensureUniqueCode(String code, UUID excludedId) {
        if (repository.existsSourceByCode(code, excludedId)) {
            throw new ConflictException("A source with the same code already exists.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <T> PageResult<T> page(List<T> items, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }
        int fromIndex = (int) Math.min((long) page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = items.isEmpty() ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PageResult<>(
                items.subList(fromIndex, toIndex),
                page,
                size,
                items.size(),
                totalPages);
    }
}
