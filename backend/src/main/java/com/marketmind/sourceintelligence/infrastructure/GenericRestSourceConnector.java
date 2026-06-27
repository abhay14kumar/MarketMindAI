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
import java.util.regex.Pattern;

import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.discovery.infrastructure.DiscoveryCrawlerException;
import com.marketmind.discovery.infrastructure.DiscoveryProperties;
import com.marketmind.sourceintelligence.application.SourceConnector;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceFormat;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class GenericRestSourceConnector implements SourceConnector {

    private static final Pattern PDF_URL = Pattern.compile(
            "https?://[^\\s\"'<>]+?\\.pdf(?:\\?[^\\s\"'<>]*)?",
            Pattern.CASE_INSENSITIVE);
    private final HttpClient httpClient;
    private final DiscoveryProperties properties;

    public GenericRestSourceConnector(
            @Qualifier("discoveryHttpClient") HttpClient httpClient,
            DiscoveryProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @Override public SourceConnectorType type() { return SourceConnectorType.GENERIC_REST; }
    @Override public SourceTrustTier trustTier() { return SourceTrustTier.THIRD_PARTY; }
    @Override public Set<SourceFormat> supportedFormats() {
        return Set.of(SourceFormat.REST, SourceFormat.JSON, SourceFormat.XML, SourceFormat.PDF);
    }
    @Override public Set<DiscoveredDocumentType> supportedDocumentTypes() {
        return Set.of(DiscoveredDocumentType.values());
    }
    @Override public int selectionScore(ConnectorRequest request) {
        if (request.sourceUrl() == null) return 0;
        String path = request.sourceUrl().getPath() == null
                ? "" : request.sourceUrl().getPath().toLowerCase(Locale.ROOT);
        return path.contains("/api/") || path.endsWith(".json") ? 600 : 10;
    }

    @Override
    public ConnectorResult discover(ConnectorRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder(request.sourceUrl())
                .timeout(properties.timeout())
                .header("User-Agent", properties.userAgent())
                .header("Accept", "application/json,application/xml,text/xml")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new DiscoveryCrawlerException(
                        "REST source returned HTTP " + response.statusCode() + ".");
            }
            String body = new String(response.body(), StandardCharsets.UTF_8);
            var matcher = PDF_URL.matcher(body);
            var documents = new LinkedHashMap<String, ConnectorDocument>();
            int matches = 0;
            while (matcher.find()) {
                matches++;
                URI uri = URI.create(matcher.group());
                documents.putIfAbsent(uri.toString(), new ConnectorDocument(uri, fileName(uri)));
                if (documents.size() >= request.maxDocuments()) break;
            }
            return new ConnectorResult(
                    documents.values().stream().toList(),
                    type(),
                    true,
                    true,
                    response.statusCode(),
                    response.body().length,
                    matches,
                    documents.size(),
                    Math.max(0, matches - documents.size()),
                    documents.isEmpty()
                            ? "REST payload was fetched but contained no absolute PDF URLs."
                            : "REST payload documents were discovered.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DiscoveryCrawlerException("REST discovery was interrupted.", exception);
        } catch (IOException exception) {
            throw new DiscoveryCrawlerException("Unable to fetch REST source.", exception);
        }
    }

    private String fileName(URI uri) {
        String path = uri.getPath();
        int index = path == null ? -1 : path.lastIndexOf('/');
        return index >= 0 ? path.substring(index + 1) : "PDF document";
    }
}
