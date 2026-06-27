package com.marketmind.sourceintelligence.infrastructure;

import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.discovery.infrastructure.GenericHtmlPdfCrawler;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;
import org.springframework.stereotype.Component;

@Component
public class SebiSourceConnector extends AbstractHtmlSourceConnector {
    public SebiSourceConnector(GenericHtmlPdfCrawler crawler) {
        super(crawler, SourceConnectorType.SEBI, SourceTrustTier.OFFICIAL);
    }
    @Override public int selectionScore(ConnectorRequest request) {
        return request.sourceType() == DiscoverySourceType.SEBI ? 1000 : 0;
    }
}
