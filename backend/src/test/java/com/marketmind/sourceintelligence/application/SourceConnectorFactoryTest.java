package com.marketmind.sourceintelligence.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Set;

import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceFormat;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;

import org.junit.jupiter.api.Test;

class SourceConnectorFactoryTest {

    @Test
    void shouldPrioritizeOfficialConnectorWhenScoresAreOtherwiseEqual() {
        SourceConnector thirdParty = connector(
                SourceConnectorType.GENERIC_REST, SourceTrustTier.THIRD_PARTY, 500);
        SourceConnector official = connector(
                SourceConnectorType.NSE, SourceTrustTier.OFFICIAL, 500);
        SourceConnectorFactory factory = new SourceConnectorFactory(
                List.of(thirdParty, official));

        SourceConnector selected = factory.select(new SourceConnector.ConnectorRequest(
                DiscoverySourceType.NSE,
                URI.create("https://www.nseindia.com"),
                null,
                20));

        assertThat(selected.type()).isEqualTo(SourceConnectorType.NSE);
    }

    private SourceConnector connector(
            SourceConnectorType type,
            SourceTrustTier trustTier,
            int score) {
        return new SourceConnector() {
            @Override public SourceConnectorType type() { return type; }
            @Override public SourceTrustTier trustTier() { return trustTier; }
            @Override public Set<SourceFormat> supportedFormats() {
                return Set.of(SourceFormat.JSON);
            }
            @Override public Set<DiscoveredDocumentType> supportedDocumentTypes() {
                return Set.of(DiscoveredDocumentType.UNKNOWN);
            }
            @Override public int selectionScore(ConnectorRequest request) { return score; }
            @Override public ConnectorResult discover(ConnectorRequest request) {
                return new ConnectorResult(
                        List.of(), type, true, true, 200, 0, 0, 0, 0, "ok");
            }
        };
    }
}
