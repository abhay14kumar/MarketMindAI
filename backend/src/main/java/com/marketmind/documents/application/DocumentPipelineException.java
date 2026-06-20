package com.marketmind.documents.application;

import com.marketmind.common.exception.ApiException;
import com.marketmind.common.exception.ErrorCode;

import org.springframework.http.HttpStatus;

public class DocumentPipelineException extends ApiException {

    private static final long serialVersionUID = 1L;

    public DocumentPipelineException(
            HttpStatus status,
            ErrorCode errorCode,
            String message) {
        super(status, errorCode, message);
    }

    public DocumentPipelineException(
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            Throwable cause) {
        super(status, errorCode, message, cause);
    }

    public static DocumentPipelineException invalidUrl(String message) {
        return new DocumentPipelineException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_DOCUMENT_URL,
                message);
    }

    public static DocumentPipelineException downloadFailed(String message) {
        return new DocumentPipelineException(
                HttpStatus.BAD_GATEWAY,
                ErrorCode.DOWNLOAD_FAILED,
                message);
    }

    public static DocumentPipelineException downloadFailed(String message, Throwable cause) {
        return new DocumentPipelineException(
                HttpStatus.BAD_GATEWAY,
                ErrorCode.DOWNLOAD_FAILED,
                message,
                cause);
    }

    public static DocumentPipelineException timeout(Throwable cause) {
        return new DocumentPipelineException(
                HttpStatus.GATEWAY_TIMEOUT,
                ErrorCode.DOWNLOAD_TIMEOUT,
                "The document download timed out.",
                cause);
    }

    public static DocumentPipelineException fileTooLarge(long maxBytes) {
        return new DocumentPipelineException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ErrorCode.FILE_TOO_LARGE,
                "The document exceeds the maximum allowed size of " + maxBytes + " bytes.");
    }

    public static DocumentPipelineException duplicate(String checksum) {
        return new DocumentPipelineException(
                HttpStatus.CONFLICT,
                ErrorCode.DUPLICATE_DOCUMENT,
                "A document with checksum " + checksum + " already exists.");
    }

    public static DocumentPipelineException storageFailure(String message, Throwable cause) {
        return new DocumentPipelineException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.STORAGE_FAILURE,
                message,
                cause);
    }
}
