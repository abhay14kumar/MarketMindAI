package com.marketmind.documents.infrastructure;

public class DocumentParsingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DocumentParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
