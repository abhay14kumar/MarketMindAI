package com.marketmind.discovery.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.marketmind.discovery.application.SourceCrawler;
import com.marketmind.discovery.domain.DiscoverySourceType;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class GenericHtmlPdfCrawler implements SourceCrawler {

    private static final Logger log = LoggerFactory.getLogger(GenericHtmlPdfCrawler.class);
    static final String CRAWLER_TYPE = "GENERIC_HTML_PDF";

    private final HttpClient httpClient;
    private final DiscoveryProperties properties;

    public GenericHtmlPdfCrawler(
            @Qualifier("discoveryHttpClient") HttpClient httpClient,
            DiscoveryProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @Override
    public DiscoverySourceType sourceType() {
        return DiscoverySourceType.COMPANY_IR;
    }

    @Override
    public CrawlResult crawl(CrawlRequest request) {
        validateSourceUrl(request.sourceUrl());
        HttpRequest httpRequest = HttpRequest.newBuilder(request.sourceUrl())
                .timeout(properties.timeout())
                .header("User-Agent", properties.userAgent())
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "en-US,en;q=0.9")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn(
                        "Discovery source returned non-success status sourceUrl={} httpStatus={}",
                        response.uri(),
                        response.statusCode());
                throw new DiscoveryCrawlerException(
                        "Source returned HTTP " + response.statusCode() + ".");
            }
            if (response.body().length > properties.maxHtmlBytes()) {
                throw new DiscoveryCrawlerException(
                        "Source HTML exceeded the configured size limit.");
            }
            CrawlResult result = analyzeHtml(
                    response.uri(),
                    new String(response.body(), StandardCharsets.UTF_8),
                    request.maxDocuments(),
                    response.statusCode(),
                    response.body().length);
            log.info(
                    "Discovery source fetched sourceUrl={} httpStatus={} fetchedHtmlBytes={} linksScanned={} pdfLinksFound={}",
                    response.uri(),
                    response.statusCode(),
                    response.body().length,
                    result.totalLinksFound(),
                    result.pdfLinksFound());
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DiscoveryCrawlerException(
                    "Discovery request was interrupted.", exception);
        } catch (IOException exception) {
            throw new DiscoveryCrawlerException(
                    "Unable to fetch discovery source.", exception);
        }
    }

    static List<CrawledDocument> extractPdfLinks(
            URI sourceUrl,
            String html,
            int maxDocuments) {
        return analyzeHtml(sourceUrl, html, maxDocuments, 200, html.getBytes(StandardCharsets.UTF_8).length)
                .documents();
    }

    static CrawlResult analyzeHtml(
            URI sourceUrl,
            String html,
            int maxDocuments,
            int httpStatus,
            long fetchedHtmlBytes) {
        Document document = Jsoup.parse(html, sourceUrl.toString());
        Map<String, CrawledDocument> links = new LinkedHashMap<>();
        int totalLinks = 0;
        int pdfLinks = 0;
        int skippedLinks = 0;
        for (Element anchor : document.select("a[href]")) {
            totalLinks++;
            String absoluteUrl = anchor.absUrl("href");
            if (absoluteUrl.isBlank()) {
                skippedLinks++;
                continue;
            }
            URI documentUrl;
            try {
                documentUrl = URI.create(absoluteUrl);
            } catch (IllegalArgumentException ignored) {
                skippedLinks++;
                continue;
            }
            if (!isPdf(documentUrl)) {
                skippedLinks++;
                continue;
            }
            pdfLinks++;
            String title = anchor.text().strip();
            if (title.isBlank()) {
                title = fileName(documentUrl);
            }
            if (links.size() < maxDocuments) {
                links.putIfAbsent(documentUrl.toString(), new CrawledDocument(
                        documentUrl, title));
            }
        }
        boolean nseSource = sourceUrl.getHost() != null
                && sourceUrl.getHost().toLowerCase(Locale.ROOT).contains("nseindia.com");
        String message = nseSource
                ? "NSE pages often require source-specific APIs or browser/session handling. "
                        + "Generic PDF link extraction may return zero results."
                : links.isEmpty()
                        ? "The source was reached, but no direct PDF links were found in the fetched HTML."
                        : "Direct PDF links were discovered successfully.";
        return new CrawlResult(
                List.copyOf(links.values()),
                CRAWLER_TYPE,
                true,
                true,
                httpStatus,
                fetchedHtmlBytes,
                totalLinks,
                pdfLinks,
                skippedLinks,
                message);
    }

    private static boolean isPdf(URI uri) {
        String path = uri.getPath();
        return path != null && path.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private static String fileName(URI uri) {
        String path = uri.getPath();
        int index = path == null ? -1 : path.lastIndexOf('/');
        return index >= 0 && index < path.length() - 1
                ? path.substring(index + 1)
                : "PDF document";
    }

    private void validateSourceUrl(URI sourceUrl) {
        if (sourceUrl == null) {
            throw new IllegalArgumentException(
                    "sourceUrl is required for URL discovery.");
        }
        String scheme = sourceUrl.getScheme() == null
                ? ""
                : sourceUrl.getScheme().toLowerCase(Locale.ROOT);
        if ((!scheme.equals("http") && !scheme.equals("https"))
                || sourceUrl.getHost() == null
                || sourceUrl.getHost().isBlank()
                || sourceUrl.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "sourceUrl must be an absolute HTTP or HTTPS URL.");
        }
        if (isLocalOrPrivateHost(sourceUrl.getHost())) {
            throw new IllegalArgumentException(
                    "sourceUrl must reference a public trusted source.");
        }
    }

    private boolean isLocalOrPrivateHost(String host) {
        String value = host.toLowerCase(Locale.ROOT);
        if (value.equals("localhost")
                || value.endsWith(".localhost")
                || value.equals("::1")
                || value.equals("0.0.0.0")
                || value.startsWith("127.")
                || value.startsWith("10.")
                || value.startsWith("192.168.")
                || value.startsWith("169.254.")) {
            return true;
        }
        if (!value.startsWith("172.")) {
            return false;
        }
        String[] parts = value.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int secondOctet = Integer.parseInt(parts[1]);
            return secondOctet >= 16 && secondOctet <= 31;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
