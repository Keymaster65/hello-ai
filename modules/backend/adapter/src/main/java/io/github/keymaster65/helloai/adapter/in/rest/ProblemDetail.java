package io.github.keymaster65.helloai.adapter.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Error payload of the API, shaped after
 * <a href="https://www.rfc-editor.org/rfc/rfc9457.html">RFC 9457</a> and served as
 * {@code application/problem+json} (see ADR 0046).
 *
 * <p>The five members of the RFC are complemented by {@code fieldErrors}, an extension member.
 * It is described in the contract instead of living in an untyped property map, so the generated
 * TypeScript types keep it (see {@code docs/system/api.adoc}).
 *
 * <p>Spring ships a class of the same name; it is deliberately not used here, because its
 * extension members would drop out of the contract. The name follows the specification.
 *
 * @param type        URI reference identifying the problem type
 * @param title       human-readable summary of the problem type, constant across occurrences
 * @param status      HTTP status code
 * @param detail      human-readable explanation of this occurrence
 * @param instance    URI reference of the request that produced the problem
 * @param fieldErrors per-field validation errors; empty unless a validation failed, never null
 */
@Schema(name = "ProblemDetail", description = "Error payload following RFC 9457")
public record ProblemDetail(
        @Schema(description = "URI reference identifying the problem type",
                example = "/recipes/docs/#problem-not-found",
                requiredMode = Schema.RequiredMode.REQUIRED) String type,
        @Schema(description = "Human-readable summary of the problem type", example = "Recipe not found",
                requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(description = "HTTP status code", example = "404",
                requiredMode = Schema.RequiredMode.REQUIRED) int status,
        @Schema(description = "Human-readable explanation of this occurrence",
                example = "Recipe 42 not found",
                requiredMode = Schema.RequiredMode.REQUIRED) String detail,
        @Schema(description = "URI reference of the request that produced the problem",
                example = "/recipes/api/recipes/42",
                requiredMode = Schema.RequiredMode.REQUIRED) String instance,
        @Schema(description = "Per-field validation errors; empty unless a validation failed",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<FieldError> fieldErrors) {

    /**
     * Starts the curried construction of a {@link ProblemDetail} (see ADR 0021).
     *
     * @return the first step of the curried factory
     */
    public static TypeStep curried() {
        return type -> title -> status -> detail -> instance -> fieldErrors ->
                new ProblemDetail(type, title, status, detail, instance, fieldErrors);
    }

    /** Step 1 of {@link #curried()}: the type URI. */
    @FunctionalInterface
    public interface TypeStep {

        /**
         * @param type URI reference identifying the problem type
         * @return the next step
         */
        TitleStep type(String type);
    }

    /** Step 2 of {@link #curried()}: the title. */
    @FunctionalInterface
    public interface TitleStep {

        /**
         * @param title human-readable summary of the problem type
         * @return the next step
         */
        StatusStep title(String title);
    }

    /** Step 3 of {@link #curried()}: the HTTP status code. */
    @FunctionalInterface
    public interface StatusStep {

        /**
         * @param status HTTP status code
         * @return the next step
         */
        DetailStep status(int status);
    }

    /** Step 4 of {@link #curried()}: the detail of this occurrence. */
    @FunctionalInterface
    public interface DetailStep {

        /**
         * @param detail human-readable explanation of this occurrence
         * @return the next step
         */
        InstanceStep detail(String detail);
    }

    /** Step 5 of {@link #curried()}: the request that produced the problem. */
    @FunctionalInterface
    public interface InstanceStep {

        /**
         * @param instance URI reference of the request
         * @return the next step
         */
        FieldErrorsStep instance(String instance);
    }

    /** Step 6 of {@link #curried()}: the field errors, completing the payload. */
    @FunctionalInterface
    public interface FieldErrorsStep {

        /**
         * @param fieldErrors per-field validation errors (empty unless a validation failed)
         * @return the finished {@link ProblemDetail}
         */
        ProblemDetail fieldErrors(List<FieldError> fieldErrors);
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
