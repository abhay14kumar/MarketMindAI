package com.marketmind.sources.application;

import java.net.URI;

import com.marketmind.sources.domain.CapabilityStatus;

public interface PdfCapabilityChecker {

    PdfCapabilityResult check(URI samplePdfUrl);

    record PdfCapabilityResult(CapabilityStatus status, String message) {
    }
}
