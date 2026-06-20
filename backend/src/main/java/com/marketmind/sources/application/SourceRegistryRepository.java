package com.marketmind.sources.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.sources.domain.SourceCapability;
import com.marketmind.sources.domain.SourceHealth;
import com.marketmind.sources.domain.SourceRegistry;
import com.marketmind.sources.domain.SourceValidationHistory;

public interface SourceRegistryRepository {

    List<SourceRegistry> findAllSources();

    Optional<SourceRegistry> findSourceById(UUID id);

    boolean existsSourceByCode(String code, UUID excludedId);

    SourceRegistry saveSource(SourceRegistry source);

    void deleteSource(UUID id);

    List<SourceHealth> findAllHealth();

    SourceHealth saveHealth(SourceHealth health);

    List<SourceCapability> findAllCapabilities();

    void replaceCapabilities(UUID sourceId, List<SourceCapability> capabilities);

    SourceValidationHistory saveValidation(SourceValidationHistory validation);
}
