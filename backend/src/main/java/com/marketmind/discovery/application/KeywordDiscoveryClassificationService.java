package com.marketmind.discovery.application;

import java.net.URI;
import java.util.Locale;

import com.marketmind.discovery.domain.DiscoveredDocumentType;

import org.springframework.stereotype.Service;

@Service
public class KeywordDiscoveryClassificationService
        implements DiscoveryClassificationService {

    @Override
    public DiscoveredDocumentType classify(String title, URI documentUrl) {
        String value = ((title == null ? "" : title)
                + " "
                + (documentUrl == null ? "" : documentUrl.toString()))
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ');
        if (value.contains("annual report")) {
            return DiscoveredDocumentType.ANNUAL_REPORT;
        }
        if (value.contains("quarterly") || value.contains("result")) {
            return DiscoveredDocumentType.QUARTERLY_RESULT;
        }
        if (value.contains("presentation")) {
            return DiscoveredDocumentType.INVESTOR_PRESENTATION;
        }
        if (value.contains("circular")) {
            return DiscoveredDocumentType.CIRCULAR;
        }
        if (value.contains("announcement")) {
            return DiscoveredDocumentType.ANNOUNCEMENT;
        }
        return DiscoveredDocumentType.UNKNOWN;
    }
}
