package com.marketmind.discovery.dto;

import java.util.List;

public record DiscoveryJobDetailResponse(
        DiscoveryJobResponse job,
        List<DiscoverySourceRunResponse> sourceRuns) {

    public DiscoveryJobDetailResponse {
        sourceRuns = List.copyOf(sourceRuns);
    }
}
