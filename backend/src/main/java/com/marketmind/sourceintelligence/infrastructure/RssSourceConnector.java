package com.marketmind.sourceintelligence.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;

import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.discovery.infrastructure.DiscoveryCrawlerException;
import com.marketmind.discovery.infrastructure.DiscoveryProperties;
import com.marketmind.sourceintelligence.application.SourceConnector;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceFormat;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class RssSourceConnector implements SourceConnector {

    private final HttpClient httpClient;
    private final DiscoveryProperties properties;

    public RssSourceConnector(
            @Qualifier("discoveryHttpClient") HttpClient httpClient,
            DiscoveryProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @Override public SourceConnectorType type() { return SourceConnectorType.RSS; }
    @Override public SourceTrustTier trustTier() { return SourceTrustTier.AUTHORIZED; }
    @Override public Set<SourceFormat> supportedFormats() {
        return Set.of(SourceFormat.RSS, SourceFormat.XML, SourceFormat.PDF);
    }
    @Override public Set<DiscoveredDocumentType> supportedDocumentTypes() {
        return Set.of(DiscoveredDocumentType.values());
    }
    @Override public int selectionScore(ConnectorRequest request) {
        if (request.sourceUrl() == null) return 0;
        String value = request.sourceUrl().toString().toLowerCase(Locale.ROOT);
        return value.contains("rss") || value.endsWith(".xml") ? 700 : 0;
    }

    @Override
    public ConnectorResult discover(ConnectorRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder(request.sourceUrl())
                .timeout(properties.timeout())
                .header("User-Agent", properties.userAgent())
                .header("Accept", "application/rss+xml,application/atom+xml,application/xml,text/xml")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new DiscoveryCrawlerException(
                        "RSS source returned HTTP " + response.statusCode() + ".");
            }
            String xml = new String(response.body(), StandardCharsets.UTF_8);
            var document = Jsoup.parse(xml, response.uri().toString(), org.jsoup.parser.Parser.xmlParser());
            var links = new LinkedHashMap<String, ConnectorDocument>();
            int scanned = 0;
            for (var element : document.select("enclosure[url], link[href], link")) {
                scanned++;
                String raw = element.hasAttr("url")
                        ? element.attr("url")
                        : element.hasAttr("href") ? element.attr("href") : element.text();
                if (raw.isBlank()) continue;
                URI uri = response.uri().resolve(raw);
                if (uri.getPath() == null
                        || !uri.getPath().toLowerCase(Locale.ROOT).endsWith(".pdf")) continue;
                links.putIfAbsent(uri.toString(), new ConnectorDocument(uri, element.attr("title")));
                if (links.size() >= request.maxDocuments()) break;
            }
            return new ConnectorResult(
                    links.values().stream().toList(),
                    type(),
                    true,
                    true,
                    response.statusCode(),
                    response.body().length,
                    scanned,
                    links.size(),
                    Math.max(0, scanned - links.size()),
                    links.isEmpty()
                            ? "RSS feed was fetched but contained no direct PDF entries."
                            : "RSS feed documents were discovered.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DiscoveryCrawlerException("RSS discovery was interrupted.", exception);
        } catch (IOException exception) {
            throw new DiscoveryCrawlerException("Unable to fetch RSS source.", exception);
        }
    }
}
