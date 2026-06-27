package com.marketmind.sourceintelligence.application;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class SourceConnectorFactory {

    private final List<SourceConnector> connectors;

    public SourceConnectorFactory(List<SourceConnector> connectors) {
        this.connectors = List.copyOf(connectors);
    }

    public SourceConnector select(SourceConnector.ConnectorRequest request) {
        return connectors.stream()
                .map(connector -> new Candidate(
                        connector,
                        connector.selectionScore(request)
                                + trustBonus(connector)))
                .filter(candidate -> candidate.score() > 0)
                .max(Comparator.comparingInt(Candidate::score))
                .map(Candidate::connector)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No source connector supports the requested source."));
    }

    public List<SourceConnector> connectors() {
        return connectors;
    }

    private int trustBonus(SourceConnector connector) {
        return switch (connector.trustTier()) {
            case OFFICIAL -> 100;
            case AUTHORIZED -> 50;
            case THIRD_PARTY -> 10;
            case TEST -> 0;
        };
    }

    private record Candidate(SourceConnector connector, int score) {
    }
}
