package com.marketmind.sourceintelligence.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.sourceintelligence.domain.SourceActivity;
import com.marketmind.sourceintelligence.domain.SourceIntelligenceProfile;

public interface SourceIntelligenceRepository {
    Optional<SourceIntelligenceProfile> findProfile(UUID sourceId);
    SourceIntelligenceProfile saveProfile(SourceIntelligenceProfile profile);
    SourceActivity saveActivity(SourceActivity activity);
    List<SourceActivity> findActivity(int limit);
    List<SourceCoverageRow> coverage();
    SourceIntelligenceMetrics metrics();
}
