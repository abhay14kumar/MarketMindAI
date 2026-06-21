package com.marketmind.discovery.application;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class DefaultDiscoveryDeduplicationService
        implements DiscoveryDeduplicationService {

    @Override
    public String normalize(URI documentUrl) {
        if (documentUrl == null || documentUrl.getHost() == null) {
            throw new IllegalArgumentException("Document URL must be an absolute HTTP URL.");
        }
        String scheme = documentUrl.getScheme() == null
                ? ""
                : documentUrl.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Only HTTP and HTTPS document URLs are supported.");
        }
        int port = documentUrl.getPort();
        if ((scheme.equals("http") && port == 80)
                || (scheme.equals("https") && port == 443)) {
            port = -1;
        }
        String path = documentUrl.getPath();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        try {
            return new URI(
                    scheme,
                    null,
                    documentUrl.getHost().toLowerCase(Locale.ROOT),
                    port,
                    path,
                    documentUrl.getQuery(),
                    null)
                    .normalize()
                    .toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Document URL cannot be normalized.", exception);
        }
    }
}
