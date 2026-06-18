package com.marketmind.common.exception;

public record FieldViolation(String field, String code, String message) {
}
