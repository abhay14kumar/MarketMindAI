package com.marketmind.discovery.application;

import java.net.URI;

public interface DiscoveryDeduplicationService {

    String normalize(URI documentUrl);
}
