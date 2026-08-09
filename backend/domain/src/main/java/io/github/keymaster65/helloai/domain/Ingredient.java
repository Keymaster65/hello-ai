package io.github.keymaster65.helloai.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A single ingredient of a recipe.
 *
 * @param name     human readable name, must not be {@code null} or blank
 * @param quantity amount needed, may be {@code null} if not applicable
 * @param unit     unit of the quantity (e.g. "g", "ml"), may be {@code null}
 */
public record Ingredient(String name, BigDecimal quantity, String unit) {

    public Ingredient {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("ingredient name must not be blank");
        }
    }
}
