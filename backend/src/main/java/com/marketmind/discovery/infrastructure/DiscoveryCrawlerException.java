package com.marketmind.discovery.infrastructure;

public class DiscoveryCrawlerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DiscoveryCrawlerException(String message) {
        super(message);
    }

    public DiscoveryCrawlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
