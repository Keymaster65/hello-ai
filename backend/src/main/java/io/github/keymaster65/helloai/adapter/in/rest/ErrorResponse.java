package io.github.keymaster65.helloai.adapter.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Uniform error payload returned by the API.
 *
 * @param status       HTTP status code
 * @param error        short, machine-readable error label
 * @param message      human-readable description
 * @param fieldErrors  per-field validation errors (empty unless a validation failed)
 */
@Schema(name = "ErrorResponse", description = "Uniform error payload")
public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "404",
                requiredMode = Schema.RequiredMode.REQUIRED) int status,
        @Schema(description = "Short, machine-readable error label", example = "NOT_FOUND",
                requiredMode = Schema.RequiredMode.REQUIRED) String error,
        @Schema(description = "Human-readable description", example = "Recipe 42 not found",
                requiredMode = Schema.RequiredMode.REQUIRED) String message,
        @Schema(description = "Per-field validation errors; empty unless a validation failed",
                requiredMode = Schema.RequiredMode.REQUIRED)
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
    @Schema(name = "FieldError", description = "A single field-level validation error")
    public record FieldError(
            @Schema(description = "Name of the offending field", example = "title",
                    requiredMode = Schema.RequiredMode.REQUIRED) String field,
            @Schema(description = "Validation message", example = "must not be blank",
                    requiredMode = Schema.RequiredMode.REQUIRED) String message) {
    }
}
