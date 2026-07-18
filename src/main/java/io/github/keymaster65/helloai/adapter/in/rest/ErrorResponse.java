package io.github.keymaster65.helloai.adapter.in.rest;

import java.util.List;

/**
 * Uniform error payload returned by the API.
 *
 * @param status       HTTP status code
 * @param error        short, machine-readable error label
 * @param message      human-readable description
 * @param fieldErrors  per-field validation errors (empty unless a validation failed)
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors) {

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, List.of());
    }

    /**
     * A single field-level validation error.
     *
     * @param field   name of the offending field
     * @param message validation message
     */
    public record FieldError(String field, String message) {
    }
}
