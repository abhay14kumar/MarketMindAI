package com.marketmind.sourceintelligence.infrastructure;

import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.discovery.infrastructure.GenericHtmlPdfCrawler;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;
import org.springframework.stereotype.Component;

@Component
public class CompanyIrSourceConnector extends AbstractHtmlSourceConnector {
    public CompanyIrSourceConnector(GenericHtmlPdfCrawler crawler) {
        super(crawler, SourceConnectorType.COMPANY_IR, SourceTrustTier.OFFICIAL);
    }
    @Override public int selectionScore(ConnectorRequest request) {
        if (request.sourceUrl() != null) {
            String value = request.sourceUrl().toString().toLowerCase(java.util.Locale.ROOT);
            if (value.contains("rss") || value.endsWith(".xml")
                    || value.contains("/api/") || value.endsWith(".json")) {
                return 50;
            }
        }
        return request.sourceType() == DiscoverySourceType.COMPANY_IR ? 800 : 100;
    }
}
