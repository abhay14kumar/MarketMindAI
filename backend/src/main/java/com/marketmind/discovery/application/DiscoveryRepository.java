package com.marketmind.discovery.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.discovery.domain.DiscoveredDocument;
import com.marketmind.discovery.domain.DiscoveryJob;
import com.marketmind.discovery.domain.DiscoverySourceRun;

public interface DiscoveryRepository {

    DiscoveryJob saveJob(DiscoveryJob job);

    DiscoverySourceRun saveSourceRun(DiscoverySourceRun sourceRun);

    DiscoveredDocument saveDocument(DiscoveredDocument document);

    Optional<DiscoveryJob> findJob(UUID id);

    PageResult<DiscoveryJob> findJobs(int page, int size);

    List<DiscoverySourceRun> findSourceRuns(UUID jobId);

    Optional<DiscoveredDocument> findDocument(UUID id);

    Optional<DiscoveredDocument> findByNormalizedUrl(String normalizedUrl);

    PageResult<DiscoveredDocument> findDocuments(
            DiscoveryDocumentFilter filter,
            int page,
            int size);
}
