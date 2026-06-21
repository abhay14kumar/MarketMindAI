package com.marketmind.documents.infrastructure;

public class UnsupportedDocumentFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnsupportedDocumentFormatException(String message) {
        super(message);
    }
}
