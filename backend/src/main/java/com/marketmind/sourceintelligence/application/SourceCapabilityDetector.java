package com.marketmind.sourceintelligence.application;

import java.net.URI;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import com.marketmind.sourceintelligence.domain.SourceFormat;

import org.springframework.stereotype.Component;

@Component
public class SourceCapabilityDetector {

    public Set<SourceFormat> detect(URI sourceUrl, SourceConnector connector) {
        EnumSet<SourceFormat> formats = EnumSet.copyOf(connector.supportedFormats());
        if (sourceUrl == null) {
            return Set.copyOf(formats);
        }
        String value = sourceUrl.toString().toLowerCase(Locale.ROOT);
        if (value.contains("graphql")) formats.add(SourceFormat.GRAPHQL);
        if (value.contains("rss") || value.endsWith(".xml")) {
            formats.add(SourceFormat.RSS);
            formats.add(SourceFormat.XML);
        }
        if (value.contains("/api/") || value.endsWith(".json")) {
            formats.add(SourceFormat.REST);
            formats.add(SourceFormat.JSON);
        }
        if (value.endsWith(".zip")) formats.add(SourceFormat.ZIP);
        if (value.endsWith(".pdf")) formats.add(SourceFormat.PDF);
        return Set.copyOf(formats);
    }
}
