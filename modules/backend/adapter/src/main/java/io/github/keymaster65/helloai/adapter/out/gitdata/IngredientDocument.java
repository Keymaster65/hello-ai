package io.github.keymaster65.helloai.adapter.out.gitdata;

import java.math.BigDecimal;

/**
 * One row of the {@code ingredients} entity as it is stored in a single JSON file (ADR 0053).
 *
 * <p>The components mirror the columns of the {@code ingredient} table, including the foreign key:
 * a file knows which recipe it belongs to, so the entity can be read without its parent. Unlike the
 * domain {@link io.github.keymaster65.helloai.domain.model.Ingredient}, the position is explicit
 * here &ndash; a directory has no order.
 *
 * @param id       identifier, unique within the entity
 * @param recipeId identifier of the recipe this ingredient belongs to
 * @param position 1-based order within that recipe
 * @param name     human readable name
 * @param quantity amount needed, may be {@code null}
 * @param unit     unit of the quantity, may be {@code null}
 */
record IngredientDocument(
        Long id,
        Long recipeId,
        Integer position,
        String name,
        BigDecimal quantity,
        String unit) {

    /**
     * Starts the curried construction of an {@link IngredientDocument} (see ADR 0021).
     *
     * @return the first step of the curried factory
     */
    public static IdStep curried() {
        return id -> recipeId -> position -> name -> quantity -> unit ->
                new IngredientDocument(id, recipeId, position, name, quantity, unit);
    }

    /** Step 1 of {@link #curried()}: the identifier. */
    @FunctionalInterface
    public interface IdStep {

        /**
         * @param id identifier, unique within the entity
         * @return the next step
         */
        RecipeIdStep id(Long id);
    }

    /** Step 2 of {@link #curried()}: the foreign key. */
    @FunctionalInterface
    public interface RecipeIdStep {

        /**
         * @param recipeId identifier of the recipe this ingredient belongs to
         * @return the next step
         */
        PositionStep recipeId(Long recipeId);
    }

    /** Step 3 of {@link #curried()}: the position. */
    @FunctionalInterface
    public interface PositionStep {

        /**
         * @param position 1-based order within that recipe
         * @return the next step
         */
        NameStep position(Integer position);
    }

    /** Step 4 of {@link #curried()}: the name. */
    @FunctionalInterface
    public interface NameStep {

        /**
         * @param name human readable name
         * @return the next step
         */
        QuantityStep name(String name);
    }

    /** Step 5 of {@link #curried()}: the quantity. */
    @FunctionalInterface
    public interface QuantityStep {

        /**
         * @param quantity amount needed, may be {@code null}
         * @return the next step
         */
        UnitStep quantity(BigDecimal quantity);
    }

    /** Step 6 of {@link #curried()}: the unit, completing the document. */
    @FunctionalInterface
    public interface UnitStep {

        /**
         * @param unit unit of the quantity, may be {@code null}
         * @return the finished {@link IngredientDocument}
         */
        IngredientDocument unit(String unit);
    }
}
