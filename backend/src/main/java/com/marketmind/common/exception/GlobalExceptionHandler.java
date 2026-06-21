package com.marketmind.common.exception;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.marketmind.ai.infrastructure.AiInfrastructureException;
import com.marketmind.common.observability.CorrelationIdFilter;
import com.marketmind.documents.infrastructure.DocumentParsingException;
import com.marketmind.documents.infrastructure.UnsupportedDocumentFormatException;
import com.marketmind.marketdata.application.PriceProviderException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI PROBLEM_TYPE_BASE =
            URI.create("https://docs.marketmind.local/problems/");

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(
            ApiException exception,
            HttpServletRequest request) {
        logHandled(
                exception.getStatus(),
                exception.getErrorCode(),
                request,
                exception.getStatus().is5xxServerError() ? exception : null);
        return response(
                exception.getStatus(),
                exception.getErrorCode(),
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldViolation> violations = exception.getBindingResult()
                .getFieldErrors().stream()
                .map(error -> new FieldViolation(
                        error.getField(),
                        firstCode(error.getCodes()),
                        message(error.getDefaultMessage())))
                .toList();
        List<FieldViolation> objectViolations = exception.getBindingResult()
                .getGlobalErrors().stream()
                .map(error -> new FieldViolation(
                        "_request",
                        firstCode(error.getCodes()),
                        message(error.getDefaultMessage())))
                .toList();

        return validationResponse(
                "One or more fields are invalid.",
                concat(violations, objectViolations),
                request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(
                        violation.getPropertyPath().toString(),
                        "INVALID",
                        violation.getMessage()))
                .toList();
        return validationResponse(
                "One or more constraints were violated.", violations, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request) {
        List<FieldViolation> violations =
                exception.getParameterValidationResults().stream()
                        .flatMap(result -> result.getResolvableErrors().stream()
                                .map(error -> new FieldViolation(
                                        parameterName(result.getMethodParameter()
                                                .getParameterName()),
                                        firstCode(error.getCodes()),
                                        message(error.getDefaultMessage()))))
                        .toList();
        return validationResponse(
                "One or more request parameters are invalid.",
                violations,
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        Throwable cause = exception.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormat
                && invalidFormat.getTargetType() != null
                && invalidFormat.getTargetType().isEnum()) {
            String field = jsonField(invalidFormat);
            String rejectedValue = String.valueOf(invalidFormat.getValue());
            String allowedValues = allowedEnumValues(
                    invalidFormat.getTargetType());
            String detail = invalidEnumMessage(
                    rejectedValue, field, allowedValues);
            FieldViolation violation = new FieldViolation(
                    field,
                    ErrorCode.INVALID_ENUM_VALUE.name(),
                    detail);
            logHandled(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.INVALID_ENUM_VALUE,
                    request,
                    null);
            ProblemDetail problem = createProblem(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.INVALID_ENUM_VALUE,
                    detail,
                    request);
            setFieldErrors(problem, List.of(violation));
            return ResponseEntity.badRequest().body(problem);
        }

        logHandled(
                HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_JSON, request, null);
        return response(
                HttpStatus.BAD_REQUEST,
                ErrorCode.MALFORMED_JSON,
                "The request body contains malformed or unreadable JSON.",
                request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        boolean enumType = exception.getRequiredType() != null
                && exception.getRequiredType().isEnum();
        ErrorCode code = enumType
                ? ErrorCode.INVALID_ENUM_VALUE
                : ErrorCode.INVALID_PARAMETER;
        String detail = enumType
                ? invalidEnumMessage(
                        String.valueOf(exception.getValue()),
                        exception.getName(),
                        allowedEnumValues(exception.getRequiredType()))
                : "Invalid value for parameter: " + exception.getName() + ".";
        FieldViolation violation = new FieldViolation(
                exception.getName(),
                code.name(),
                detail);

        logHandled(HttpStatus.BAD_REQUEST, code, request, null);
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST, code, detail, request);
        setFieldErrors(problem, List.of(violation));
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        FieldViolation violation = new FieldViolation(
                exception.getParameterName(),
                ErrorCode.INVALID_PARAMETER.name(),
                "Required request parameter is missing.");
        logHandled(
                HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PARAMETER, request, null);
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_PARAMETER,
                "A required request parameter is missing.",
                request);
        setFieldErrors(problem, List.of(violation));
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ProblemDetail> handleMissingRequestPart(
            MissingServletRequestPartException exception,
            HttpServletRequest request) {
        FieldViolation violation = new FieldViolation(
                exception.getRequestPartName(),
                ErrorCode.MISSING_REQUEST_PART.name(),
                "Required multipart request part is missing.");
        logHandled(
                HttpStatus.BAD_REQUEST,
                ErrorCode.MISSING_REQUEST_PART,
                request,
                null);
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                ErrorCode.MISSING_REQUEST_PART,
                "A required multipart request part is missing.",
                request);
        setFieldErrors(problem, List.of(violation));
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleUploadSizeExceeded(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request) {
        logHandled(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ErrorCode.UPLOAD_SIZE_EXCEEDED,
                request,
                null);
        return response(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ErrorCode.UPLOAD_SIZE_EXCEEDED,
                "The uploaded file exceeds the configured size limit.",
                request);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ProblemDetail> handleMultipartFailure(
            MultipartException exception,
            HttpServletRequest request) {
        logHandled(
                HttpStatus.BAD_REQUEST,
                ErrorCode.FILE_UPLOAD_ERROR,
                request,
                null);
        return response(
                HttpStatus.BAD_REQUEST,
                ErrorCode.FILE_UPLOAD_ERROR,
                "The multipart upload could not be processed.",
                request);
    }

    @ExceptionHandler(AiInfrastructureException.class)
    public ResponseEntity<ProblemDetail> handleAiInfrastructureFailure(
            AiInfrastructureException exception,
            HttpServletRequest request) {
        logHandled(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getErrorCode(),
                request,
                exception);
        String detail = exception.getErrorCode() == ErrorCode.QDRANT_FAILURE
                || exception.getErrorCode() == ErrorCode.QDRANT_UNAVAILABLE
                ? "The vector search service is temporarily unavailable."
                : "The local AI model service is temporarily unavailable.";
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getErrorCode(),
                detail,
                request);
    }

    @ExceptionHandler(PriceProviderException.class)
    public ResponseEntity<ProblemDetail> handleExternalApiFailure(
            PriceProviderException exception,
            HttpServletRequest request) {
        logHandled(
                HttpStatus.BAD_GATEWAY,
                ErrorCode.EXTERNAL_API_FAILURE,
                request,
                exception);
        return response(
                HttpStatus.BAD_GATEWAY,
                ErrorCode.EXTERNAL_API_FAILURE,
                "An external data provider could not complete the request.",
                request);
    }

    @ExceptionHandler(UnsupportedDocumentFormatException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedDocument(
            UnsupportedDocumentFormatException exception,
            HttpServletRequest request) {
        logHandled(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ErrorCode.UNSUPPORTED_DOCUMENT_FORMAT,
                request,
                null);
        return response(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ErrorCode.UNSUPPORTED_DOCUMENT_FORMAT,
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(DocumentParsingException.class)
    public ResponseEntity<ProblemDetail> handleDocumentParsing(
            DocumentParsingException exception,
            HttpServletRequest request) {
        logHandled(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ErrorCode.DOCUMENT_PARSING_FAILED,
                request,
                exception);
        return response(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ErrorCode.DOCUMENT_PARSING_FAILED,
                "The document could not be parsed.",
                request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        logHandled(
                HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, request, null);
        return response(
                HttpStatus.NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND,
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(
            ConflictException exception,
            HttpServletRequest request) {
        logHandled(HttpStatus.CONFLICT, ErrorCode.CONFLICT, request, null);
        return response(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT,
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        logHandled(HttpStatus.CONFLICT, ErrorCode.CONFLICT, request, exception);
        return response(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT,
                "The operation conflicts with existing or referenced data.",
                request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, request, null);
        return response(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                message(exception.getMessage()),
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {
        LOGGER.atError()
                .addKeyValue("errorCode", ErrorCode.INTERNAL_ERROR)
                .addKeyValue("httpStatus", HttpStatus.INTERNAL_SERVER_ERROR.value())
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI())
                .setCause(exception)
                .log("Unhandled request exception");
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred.",
                request);
    }

    private ResponseEntity<ProblemDetail> validationResponse(
            String detail,
            List<FieldViolation> violations,
            HttpServletRequest request) {
        logHandled(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ErrorCode.VALIDATION_ERROR,
                request,
                null);
        ProblemDetail problem = createProblem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ErrorCode.VALIDATION_ERROR,
                detail,
                request);
        setFieldErrors(problem, violations);
        return ResponseEntity.unprocessableEntity().body(problem);
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            ErrorCode errorCode,
            String detail,
            HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(createProblem(status, errorCode, detail, request));
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            ErrorCode errorCode,
            String detail,
            HttpServletRequest request) {
        String correlationId = correlationId(request);
        ErrorCode publicCode = publicCode(errorCode);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(PROBLEM_TYPE_BASE.resolve(
                publicCode.name().toLowerCase().replace('_', '-')));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", publicCode.name());
        if (publicCode != errorCode) {
            problem.setProperty("legacyCode", errorCode.name());
        }
        problem.setProperty("correlationId", correlationId);
        problem.setProperty("requestId", correlationId);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    private void logHandled(
            HttpStatus status,
            ErrorCode code,
            HttpServletRequest request,
            Throwable cause) {
        ErrorCode publicCode = publicCode(code);
        var event = status.is5xxServerError()
                ? LOGGER.atWarn()
                : LOGGER.atInfo();
        event.addKeyValue("errorCode", publicCode)
                .addKeyValue("httpStatus", status.value())
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI());
        if (publicCode != code) {
            event.addKeyValue("legacyErrorCode", code);
        }
        if (cause != null) {
            event.setCause(cause);
            event.log("Request failed because a dependency or data operation failed");
        } else {
            event.log("Request rejected with a handled API error");
        }
    }

    private String correlationId(HttpServletRequest request) {
        Object attribute = request.getAttribute(
                CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        if (attribute instanceof String value && !value.isBlank()) {
            return value;
        }
        String mdcValue = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID);
        return mdcValue == null || mdcValue.isBlank()
                ? UUID.randomUUID().toString()
                : mdcValue;
    }

    private String jsonField(InvalidFormatException exception) {
        List<JsonMappingException.Reference> path = exception.getPath();
        if (path == null || path.isEmpty()) {
            return "_request";
        }
        JsonMappingException.Reference last = path.get(path.size() - 1);
        return last.getFieldName() == null
                ? "[" + last.getIndex() + "]"
                : last.getFieldName();
    }

    private void setFieldErrors(
            ProblemDetail problem,
            List<FieldViolation> violations) {
        problem.setProperty("fieldErrors", violations);
        // Compatibility alias for clients using the previous error contract.
        problem.setProperty("errors", violations);
    }

    private String invalidEnumMessage(
            String rejectedValue,
            String field,
            String allowedValues) {
        return "Invalid value '" + rejectedValue + "' for " + field
                + ". Allowed values are: " + allowedValues + ".";
    }

    private String allowedEnumValues(Class<?> enumType) {
        if (enumType == null || !enumType.isEnum()) {
            return "";
        }
        return java.util.Arrays.stream(enumType.getEnumConstants())
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    private ErrorCode publicCode(ErrorCode code) {
        return switch (code) {
            case BAD_REQUEST, INVALID_PARAMETER, MALFORMED_JSON,
                    MISSING_REQUEST_PART, FILE_UPLOAD_ERROR,
                    UPLOAD_SIZE_EXCEEDED, INVALID_DOCUMENT_URL,
                    FILE_TOO_LARGE, UNSUPPORTED_DOCUMENT_FORMAT ->
                ErrorCode.INVALID_REQUEST;
            case VALIDATION_ERROR -> ErrorCode.VALIDATION_FAILED;
            case CONFLICT, DUPLICATE_DOCUMENT -> ErrorCode.DUPLICATE_RESOURCE;
            case EXTERNAL_API_FAILURE -> ErrorCode.EXTERNAL_SERVICE_FAILURE;
            case QDRANT_UNAVAILABLE -> ErrorCode.QDRANT_FAILURE;
            case OLLAMA_UNAVAILABLE -> ErrorCode.OLLAMA_FAILURE;
            case DOWNLOAD_FAILED, DOWNLOAD_TIMEOUT, STORAGE_FAILURE ->
                ErrorCode.DOCUMENT_DOWNLOAD_FAILED;
            case DOCUMENT_PARSING_FAILED -> ErrorCode.DOCUMENT_EXTRACTION_FAILED;
            default -> code;
        };
    }

    private List<FieldViolation> concat(
            List<FieldViolation> first,
            List<FieldViolation> second) {
        return java.util.stream.Stream.concat(first.stream(), second.stream())
                .toList();
    }

    private String parameterName(String parameterName) {
        return parameterName == null ? "parameter" : parameterName;
    }

    private String firstCode(String[] codes) {
        return codes == null || codes.length == 0 ? "INVALID" : codes[0];
    }

    private String message(String value) {
        return value == null || value.isBlank() ? "Invalid value." : value;
    }
}
