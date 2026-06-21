package com.marketmind.discovery.application;

import java.util.List;

import com.marketmind.discovery.domain.DiscoveryJob;
import com.marketmind.discovery.domain.DiscoverySourceRun;

public record DiscoveryJobDetails(
        DiscoveryJob job,
        List<DiscoverySourceRun> sourceRuns) {

    public DiscoveryJobDetails {
        sourceRuns = List.copyOf(sourceRuns);
    }
}
