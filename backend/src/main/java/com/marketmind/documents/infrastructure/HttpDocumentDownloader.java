package com.marketmind.documents.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.marketmind.documents.application.DocumentPipelineException;
import com.marketmind.documents.application.Downloader;

import org.springframework.stereotype.Component;

@Component
public class HttpDocumentDownloader implements Downloader {

    private static final int MAX_REDIRECTS = 5;
    private static final String USER_AGENT = "MarketMindAI-DocumentAcquisition/1.0";

    private final HttpClient httpClient;
    private final DocumentDownloadProperties properties;
    private final Clock clock;

    public HttpDocumentDownloader(DocumentDownloadProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public DownloadResult download(DownloadRequest request) {
        return download(request.sourceUrl(), request.maxBytes(), 0);
    }

    private DownloadResult download(URI sourceUrl, int maxBytes, int redirectCount) {
        validatePublicHttpUrl(sourceUrl);
        if (redirectCount > MAX_REDIRECTS) {
            throw DocumentPipelineException.downloadFailed(
                    "The document URL exceeded the maximum redirect count.");
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(sourceUrl)
                .timeout(properties.timeout())
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/pdf,application/octet-stream,*/*;q=0.8")
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (isRedirect(response.statusCode())) {
                closeQuietly(response.body());
                URI redirectUrl = resolveRedirect(sourceUrl, response);
                return download(redirectUrl, maxBytes, redirectCount + 1);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                closeQuietly(response.body());
                throw DocumentPipelineException.downloadFailed(
                        "The document server returned HTTP status " + response.statusCode() + ".");
            }

            validateContentLength(response, maxBytes);
            Path temporaryFile = Files.createTempFile("marketmind-document-", ".download");
            try {
                long size = copyWithLimit(response.body(), temporaryFile, maxBytes);
                String contentType = response.headers()
                        .firstValue("Content-Type")
                        .map(value -> value.split(";", 2)[0].trim())
                        .filter(value -> !value.isBlank())
                        .orElse("application/octet-stream");
                return new DownloadResult(
                        temporaryFile,
                        contentType,
                        clock.instant(),
                        determineFileName(sourceUrl, response),
                        size);
            } catch (RuntimeException | IOException exception) {
                deleteQuietly(temporaryFile);
                throw exception;
            }
        } catch (HttpTimeoutException exception) {
            throw DocumentPipelineException.timeout(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw DocumentPipelineException.downloadFailed(
                    "The document download was interrupted.", exception);
        } catch (IOException exception) {
            throw DocumentPipelineException.downloadFailed(
                    "Unable to download the document.", exception);
        }
    }

    private long copyWithLimit(InputStream input, Path destination, long maxBytes)
            throws IOException {
        try (input;
                var output = Files.newOutputStream(
                        destination,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[16 * 1024];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw DocumentPipelineException.fileTooLarge(maxBytes);
                }
                output.write(buffer, 0, read);
            }
            return total;
        }
    }

    private void validateContentLength(HttpResponse<?> response, long maxBytes) {
        response.headers().firstValueAsLong("Content-Length").ifPresent(length -> {
            if (length > maxBytes) {
                throw DocumentPipelineException.fileTooLarge(maxBytes);
            }
        });
    }

    private URI resolveRedirect(URI currentUrl, HttpResponse<?> response) {
        String location = response.headers().firstValue("Location")
                .orElseThrow(() -> DocumentPipelineException.downloadFailed(
                        "The document server returned a redirect without a Location header."));
        try {
            return currentUrl.resolve(location);
        } catch (IllegalArgumentException exception) {
            throw DocumentPipelineException.invalidUrl("The redirect URL is invalid.");
        }
    }

    private void validatePublicHttpUrl(URI uri) {
        String scheme = Optional.ofNullable(uri.getScheme())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw DocumentPipelineException.invalidUrl(
                    "Only HTTP and HTTPS document URLs are supported.");
        }
        if (uri.getHost() == null || uri.getHost().isBlank() || uri.getUserInfo() != null) {
            throw DocumentPipelineException.invalidUrl(
                    "The document URL must contain a valid public host and no user information.");
        }
        List<InetAddress> addresses;
        try {
            addresses = List.of(InetAddress.getAllByName(uri.getHost()));
        } catch (UnknownHostException exception) {
            throw DocumentPipelineException.downloadFailed(
                    "The document host could not be resolved.", exception);
        }
        if (addresses.stream().anyMatch(this::isNonPublicAddress)) {
            throw DocumentPipelineException.invalidUrl(
                    "Document URLs resolving to local or private networks are not allowed.");
        }
    }

    private boolean isNonPublicAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        boolean uniqueLocalIpv6 = bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || uniqueLocalIpv6;
    }

    private String determineFileName(URI sourceUrl, HttpResponse<?> response) {
        Optional<String> disposition = response.headers().firstValue("Content-Disposition");
        if (disposition.isPresent()) {
            String fileName = fileNameFromDisposition(disposition.get());
            if (fileName != null) {
                return fileName;
            }
        }
        String path = sourceUrl.getPath();
        if (path == null || path.isBlank() || path.endsWith("/")) {
            return "document.bin";
        }
        String fileName = Path.of(path).getFileName().toString();
        return fileName.isBlank() ? "document.bin" : fileName;
    }

    private String fileNameFromDisposition(String disposition) {
        for (String part : disposition.split(";")) {
            String trimmed = part.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("filename=")) {
                String value = trimmed.substring("filename=".length()).trim();
                return value.replaceAll("^\"|\"$", "");
            }
        }
        return null;
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301
                || statusCode == 302
                || statusCode == 303
                || statusCode == 307
                || statusCode == 308;
    }

    private void closeQuietly(InputStream input) {
        try {
            input.close();
        } catch (IOException ignored) {
            // Response cleanup only; the primary HTTP error remains authoritative.
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Preserve the download error that caused cleanup.
        }
    }
}
