package io.github.keymaster65.helloai.adapter.in.rest;

import org.springframework.http.HttpStatus;

/**
 * The problem types this API answers with, as defined by RFC 9457 (see docs/prompt/api.adoc).
 *
 * <p>Each constant carries everything that is constant for its kind of problem: the identifying
 * slug, the human-readable title and the HTTP status. Only the {@code detail} of a single
 * occurrence is left to the caller, so a handler picks one type instead of three loose values.
 */
public enum ProblemType {

    /** No recipe exists for the requested identifier. */
    NOT_FOUND("not-found", "Recipe not found", HttpStatus.NOT_FOUND),

    /** The request body violates the constraints declared on the request DTO. */
    VALIDATION_FAILED("validation-failed", "Request validation failed", HttpStatus.BAD_REQUEST),

    /** The request body could not be parsed at all. */
    MALFORMED_REQUEST("malformed-request", "Malformed request body", HttpStatus.BAD_REQUEST),

    /** A domain invariant rejected an otherwise well-formed value. */
    INVALID_ARGUMENT("invalid-argument", "Invalid argument", HttpStatus.BAD_REQUEST);

    /**
     * Anchor prefix inside the delivered system documentation (docs/prompt/systemdokumentation.adoc). RFC 9457 asks the type
     * URI to yield human-readable documentation when dereferenced; this is that documentation.
     */
    private static final String DOCUMENTATION_ANCHOR = "/docs/#problem-";

    private final String slug;
    private final String title;
    private final HttpStatus status;

    ProblemType(String slug, String title, HttpStatus status) {
        this.slug = slug;
        this.title = title;
        this.status = status;
    }

    /**
     * The {@code type} member of a problem detail: a URI reference pointing at the section of the
     * system documentation that describes this kind of problem.
     *
     * @param contextPath context path of the current request, as reported by the servlet container
     *                    (see docs/prompt/api.adoc); it is read from the request rather than configured here,
     *                    so the path stays anchored in exactly one place
     * @return the type URI reference
     */
    public String uri(String contextPath) {
        return contextPath + DOCUMENTATION_ANCHOR + slug;
    }

    /** @return the identifying slug, the last segment of {@link #uri(String)} */
    public String slug() {
        return slug;
    }

    /** @return the human-readable summary of this problem type, constant across occurrences */
    public String title() {
        return title;
    }

    /** @return the HTTP status this problem type is answered with */
    public HttpStatus status() {
        return status;
    }
}
