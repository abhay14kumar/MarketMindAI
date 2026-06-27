package com.marketmind.sourceintelligence.infrastructure;

import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.discovery.infrastructure.GenericHtmlPdfCrawler;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;
import org.springframework.stereotype.Component;

@Component
public class RbiSourceConnector extends AbstractHtmlSourceConnector {
    public RbiSourceConnector(GenericHtmlPdfCrawler crawler) {
        super(crawler, SourceConnectorType.RBI, SourceTrustTier.OFFICIAL);
    }
    @Override public int selectionScore(ConnectorRequest request) {
        return request.sourceType() == DiscoverySourceType.RBI ? 1000 : 0;
    }
}
