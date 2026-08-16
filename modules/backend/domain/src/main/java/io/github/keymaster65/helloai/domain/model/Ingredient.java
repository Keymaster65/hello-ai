package io.github.keymaster65.helloai.domain.model;

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

    /**
     * Starts the curried construction of an {@link Ingredient} (see docs/prompt/architektur.adoc).
     *
     * <p>Every component is supplied by its own named step, so neighbouring components of the
     * same type cannot be swapped unnoticed:
     *
     * <pre>{@code
     * Ingredient ingredient = Ingredient.curried()
     *         .name("Spaghetti")
     *         .quantity(BigDecimal.valueOf(500))
     *         .unit("g");
     * }</pre>
     *
     * @return the first step of the curried factory
     */
    public static NameStep curried() {
        return name -> quantity -> unit -> new Ingredient(name, quantity, unit);
    }

    /** Step 1 of {@link #curried()}: the ingredient name. */
    @FunctionalInterface
    public interface NameStep {

        /**
         * @param name human readable name, must not be {@code null} or blank
         * @return the next step
         */
        QuantityStep name(String name);
    }

    /** Step 2 of {@link #curried()}: the quantity. */
    @FunctionalInterface
    public interface QuantityStep {

        /**
         * @param quantity amount needed, may be {@code null} if not applicable
         * @return the next step
         */
        UnitStep quantity(BigDecimal quantity);
    }

    /** Step 3 of {@link #curried()}: the unit, completing the ingredient. */
    @FunctionalInterface
    public interface UnitStep {

        /**
         * @param unit unit of the quantity (e.g. "g", "ml"), may be {@code null}
         * @return the finished {@link Ingredient}
         */
        Ingredient unit(String unit);
    }
}
