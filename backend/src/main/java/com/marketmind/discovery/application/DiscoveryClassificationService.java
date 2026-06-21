package com.marketmind.discovery.application;

import java.net.URI;

import com.marketmind.discovery.domain.DiscoveredDocumentType;

public interface DiscoveryClassificationService {

    DiscoveredDocumentType classify(String title, URI documentUrl);
}
