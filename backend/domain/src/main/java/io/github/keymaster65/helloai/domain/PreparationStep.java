package io.github.keymaster65.helloai.domain;

import java.util.Objects;

/**
 * A single, ordered preparation step of a recipe.
 *
 * @param position    1-based order of the step within the recipe
 * @param instruction textual instruction, must not be {@code null} or blank
 */
public record PreparationStep(int position, String instruction) {

    public PreparationStep {
        Objects.requireNonNull(instruction, "instruction must not be null");
        if (instruction.isBlank()) {
            throw new IllegalArgumentException("instruction must not be blank");
        }
        if (position < 1) {
            throw new IllegalArgumentException("position must be >= 1");
        }
    }
}
