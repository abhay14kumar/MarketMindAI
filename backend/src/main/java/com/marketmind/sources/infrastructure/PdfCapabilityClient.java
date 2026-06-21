package com.marketmind.sources.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import com.marketmind.sources.application.PdfCapabilityChecker;
import com.marketmind.sources.domain.CapabilityStatus;

import org.springframework.stereotype.Component;

@Component
public class PdfCapabilityClient implements PdfCapabilityChecker {

    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};

    private final ValidationHttpGateway gateway;
    private final SourceValidationProperties properties;

    public PdfCapabilityClient(
            ValidationHttpGateway gateway,
            SourceValidationProperties properties) {
        this.gateway = gateway;
        this.properties = properties;
    }

    @Override
    public PdfCapabilityResult check(URI samplePdfUrl) {
        if (samplePdfUrl == null) {
            return new PdfCapabilityResult(
                    CapabilityStatus.UNKNOWN,
                    "No sample PDF URL is configured.");
        }
        ValidationHttpGateway.validateHttpUrl(samplePdfUrl);
        try {
            ValidationHttpGateway.Response response =
                    gateway.execute(
                            samplePdfUrl,
                            "GET",
                            "application/pdf,*/*;q=0.5",
                            PDF_MAGIC.length);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new PdfCapabilityResult(
                        CapabilityStatus.UNSUPPORTED,
                        "Sample PDF returned HTTP status " + response.statusCode() + ".");
            }
            if (isPdfContentType(response.contentType())) {
                return new PdfCapabilityResult(
                        CapabilityStatus.SUPPORTED,
                        "Sample PDF was identified successfully.");
            }
            if (response.contentLength() > properties.maxPdfTestSizeBytes()) {
                return new PdfCapabilityResult(
                        CapabilityStatus.UNKNOWN,
                        "Sample PDF exceeds the configured inspection size.");
            }
            if (startsWithPdfMagic(response.bodyPrefix())) {
                return new PdfCapabilityResult(
                        CapabilityStatus.SUPPORTED,
                        "Sample PDF was identified successfully.");
            }
            return new PdfCapabilityResult(
                    CapabilityStatus.UNSUPPORTED,
                    "Sample response is not a PDF.");
        } catch (IOException exception) {
            return new PdfCapabilityResult(
                    CapabilityStatus.UNKNOWN,
                    "Sample PDF could not be checked.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new PdfCapabilityResult(
                    CapabilityStatus.UNKNOWN,
                    "Sample PDF check was interrupted.");
        }
    }

    boolean isPdfContentType(String contentType) {
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT).contains("application/pdf");
    }

    boolean startsWithPdfMagic(byte[] prefix) {
        if (prefix == null || prefix.length < PDF_MAGIC.length) {
            return false;
        }
        for (int index = 0; index < PDF_MAGIC.length; index++) {
            if (prefix[index] != PDF_MAGIC[index]) {
                return false;
            }
        }
        return true;
    }
}
