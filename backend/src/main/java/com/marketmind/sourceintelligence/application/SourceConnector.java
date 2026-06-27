package com.marketmind.sourceintelligence.application;

import java.net.URI;
import java.util.List;
import java.util.Set;

import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceFormat;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;

public interface SourceConnector {

    SourceConnectorType type();

    SourceTrustTier trustTier();

    Set<SourceFormat> supportedFormats();

    Set<DiscoveredDocumentType> supportedDocumentTypes();

    int selectionScore(ConnectorRequest request);

    ConnectorResult discover(ConnectorRequest request);

    record ConnectorRequest(
            DiscoverySourceType sourceType,
            URI sourceUrl,
            String companySymbol,
            int maxDocuments) {
    }

    record ConnectorDocument(URI documentUrl, String title) {
    }

    record ConnectorResult(
            List<ConnectorDocument> documents,
            SourceConnectorType connectorType,
            boolean sourceReachable,
            boolean contentFetched,
            Integer httpStatus,
            long fetchedBytes,
            int linksScanned,
            int documentLinksFound,
            int skippedLinks,
            String message) {

        public ConnectorResult {
            documents = List.copyOf(documents);
        }
    }
}
