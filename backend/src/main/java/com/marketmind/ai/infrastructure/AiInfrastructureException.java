package com.marketmind.ai.infrastructure;

import com.marketmind.common.exception.ErrorCode;

public class AiInfrastructureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    public AiInfrastructureException(String message) {
        this(ErrorCode.EXTERNAL_SERVICE_FAILURE, message);
    }

    public AiInfrastructureException(String message, Throwable cause) {
        this(ErrorCode.EXTERNAL_SERVICE_FAILURE, message, cause);
    }

    public AiInfrastructureException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AiInfrastructureException(
            ErrorCode errorCode,
            String message,
            Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
