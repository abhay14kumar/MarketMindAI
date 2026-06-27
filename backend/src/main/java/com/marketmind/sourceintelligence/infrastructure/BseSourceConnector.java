package com.marketmind.sourceintelligence.infrastructure;

import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.discovery.infrastructure.GenericHtmlPdfCrawler;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;

import org.springframework.stereotype.Component;

@Component
public class BseSourceConnector extends AbstractHtmlSourceConnector {
    public BseSourceConnector(GenericHtmlPdfCrawler crawler) {
        super(crawler, SourceConnectorType.BSE, SourceTrustTier.OFFICIAL);
    }
    @Override public int selectionScore(ConnectorRequest request) {
        return request.sourceType() == DiscoverySourceType.BSE ? 1000 : 0;
    }
}
