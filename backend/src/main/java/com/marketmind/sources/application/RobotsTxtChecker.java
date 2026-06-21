package com.marketmind.sources.application;

import java.net.URI;

public interface RobotsTxtChecker {

    RobotsTxtResult check(URI baseUrl);

    record RobotsTxtResult(boolean available, Integer httpStatus, String message) {
    }
}
