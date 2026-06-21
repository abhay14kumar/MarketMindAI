package com.marketmind.sources.application;

import java.net.URI;

public interface ReachabilityChecker {

    ReachabilityResult check(URI sourceUrl);

    record ReachabilityResult(boolean reachable, Integer httpStatus, long latencyMs, String message) {
    }
}
