package com.marketmind.common.exception;

import java.net.URI;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI PROBLEM_TYPE_BASE = URI.create("https://docs.marketmind.local/problems/");

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(
            ApiException exception,
            HttpServletRequest request) {
        ProblemDetail problem = createProblem(
                exception.getStatus(),
                exception.getErrorCode(),
                exception.getMessage(),
                request);
        return ResponseEntity.status(exception.getStatus()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(
                        error.getField(),
                        error.getCode() == null ? "INVALID" : error.getCode(),
                        error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage()))
                .toList();

        ProblemDetail problem = createProblem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ErrorCode.VALIDATION_ERROR,
                "One or more fields are invalid.",
                request);
        problem.setProperty("errors", violations);
        return ResponseEntity.unprocessableEntity().body(problem);
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

        ProblemDetail problem = createProblem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ErrorCode.VALIDATION_ERROR,
                "One or more constraints were violated.",
                request);
        problem.setProperty("errors", violations);
        return ResponseEntity.unprocessableEntity().body(problem);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request) {
        List<FieldViolation> violations = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldViolation(
                                parameterName(result.getMethodParameter().getParameterName()),
                                firstCode(error.getCodes()),
                                error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage())))
                .toList();

        ProblemDetail problem = createProblem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ErrorCode.VALIDATION_ERROR,
                "One or more request parameters are invalid.",
                request);
        problem.setProperty("errors", violations);
        return ResponseEntity.unprocessableEntity().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                "The request body is malformed or unreadable.",
                request);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                exception.getMessage(),
                request);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                "Invalid value for parameter: " + exception.getName(),
                request);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND,
                exception.getMessage(),
                request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(
            ConflictException exception,
            HttpServletRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT,
                exception.getMessage(),
                request);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        LOGGER.warn("Data integrity violation for request path {}", request.getRequestURI());

        ProblemDetail problem = createProblem(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT,
                "The operation conflicts with existing or referenced data.",
                request);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {
        LOGGER.error("Unhandled exception for request path {}", request.getRequestURI(), exception);

        ProblemDetail problem = createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred.",
                request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            ErrorCode errorCode,
            String detail,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(PROBLEM_TYPE_BASE.resolve(errorCode.name().toLowerCase().replace('_', '-')));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", errorCode.name());
        return problem;
    }

    private String parameterName(String parameterName) {
        return parameterName == null ? "parameter" : parameterName;
    }

    private String firstCode(String[] codes) {
        return codes == null || codes.length == 0 ? "INVALID" : codes[0];
    }
}
