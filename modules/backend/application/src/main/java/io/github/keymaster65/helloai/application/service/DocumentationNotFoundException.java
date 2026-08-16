package io.github.keymaster65.helloai.application.service;

/**
 * Thrown when a documentation page referenced by an identifier does not exist.
 */
public class DocumentationNotFoundException extends RuntimeException {

    private final String id;

    public DocumentationNotFoundException(String id) {
        super("Documentation page not found: " + id);
        this.id = id;
    }

    public String id() {
        return id;
    }
}
