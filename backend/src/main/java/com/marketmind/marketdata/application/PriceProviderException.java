package com.marketmind.marketdata.application;

public class PriceProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PriceProviderException(String message) {
        super(message);
    }

    public PriceProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
