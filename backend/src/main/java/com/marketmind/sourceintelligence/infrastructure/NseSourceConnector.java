package com.marketmind.sourceintelligence.infrastructure;

import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.discovery.infrastructure.GenericHtmlPdfCrawler;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;

import org.springframework.stereotype.Component;

@Component
public class NseSourceConnector extends AbstractHtmlSourceConnector {

    public NseSourceConnector(GenericHtmlPdfCrawler crawler) {
        super(crawler, SourceConnectorType.NSE, SourceTrustTier.OFFICIAL);
    }

    @Override
    public int selectionScore(ConnectorRequest request) {
        return request.sourceType() == DiscoverySourceType.NSE ? 1000
                : hostContains(request, "nseindia.com") ? 900 : 0;
    }

    @Override
    protected String connectorMessage(String crawlerMessage) {
        return crawlerMessage + " NSE source-specific API/session support remains planned.";
    }

    private boolean hostContains(ConnectorRequest request, String value) {
        return request.sourceUrl() != null && request.sourceUrl().getHost() != null
                && request.sourceUrl().getHost().contains(value);
    }
}
