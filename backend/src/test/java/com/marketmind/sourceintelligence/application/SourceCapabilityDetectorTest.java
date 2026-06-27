package com.marketmind.sourceintelligence.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Set;

import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceFormat;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;
import org.junit.jupiter.api.Test;

class SourceCapabilityDetectorTest {

    @Test
    void shouldDetectApiRssGraphqlAndZipFormatsFromUrls() {
        SourceCapabilityDetector detector = new SourceCapabilityDetector();
        SourceConnector connector = connector();

        assertThat(detector.detect(
                URI.create("https://example.com/api/filings.json"), connector))
                .contains(SourceFormat.REST, SourceFormat.JSON);
        assertThat(detector.detect(
                URI.create("https://example.com/rss/feed.xml"), connector))
                .contains(SourceFormat.RSS, SourceFormat.XML);
        assertThat(detector.detect(
                URI.create("https://example.com/graphql"), connector))
                .contains(SourceFormat.GRAPHQL);
        assertThat(detector.detect(
                URI.create("https://example.com/archive.zip"), connector))
                .contains(SourceFormat.ZIP);
    }

    private SourceConnector connector() {
        return new SourceConnector() {
            @Override public SourceConnectorType type() { return SourceConnectorType.GENERIC_REST; }
            @Override public SourceTrustTier trustTier() { return SourceTrustTier.THIRD_PARTY; }
            @Override public Set<SourceFormat> supportedFormats() { return Set.of(SourceFormat.REST); }
            @Override public Set<DiscoveredDocumentType> supportedDocumentTypes() {
                return Set.of(DiscoveredDocumentType.UNKNOWN);
            }
            @Override public int selectionScore(ConnectorRequest request) { return 1; }
            @Override public ConnectorResult discover(ConnectorRequest request) {
                return new ConnectorResult(List.of(), type(), true, true, 200, 0, 0, 0, 0, "ok");
            }
        };
    }
}
