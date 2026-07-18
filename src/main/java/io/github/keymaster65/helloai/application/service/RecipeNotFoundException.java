package io.github.keymaster65.helloai.application.service;

/**
 * Thrown when a recipe referenced by an identifier does not exist.
 */
public class RecipeNotFoundException extends RuntimeException {

    private final long id;

    public RecipeNotFoundException(long id) {
        super("Recipe not found: " + id);
        this.id = id;
    }

    public long id() {
        return id;
    }
}
