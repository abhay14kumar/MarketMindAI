package com.marketmind.ai.infrastructure;

public class AiInfrastructureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AiInfrastructureException(String message) {
        super(message);
    }

    public AiInfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
