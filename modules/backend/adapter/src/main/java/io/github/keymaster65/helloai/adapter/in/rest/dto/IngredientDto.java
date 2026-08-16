package io.github.keymaster65.helloai.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * API representation of a single ingredient.
 *
 * @param name     ingredient name (required)
 * @param quantity amount (optional, must be &gt;= 0 if present)
 * @param unit     unit of the quantity (optional)
 */
@Schema(name = "Ingredient", description = "A single ingredient of a recipe")
public record IngredientDto(
        @Schema(description = "Ingredient name", example = "Spaghetti")
        @NotBlank @Size(max = 200) String name,

        @Schema(description = "Amount of the ingredient", example = "500")
        @PositiveOrZero BigDecimal quantity,

        @Schema(description = "Unit of the quantity", example = "g")
        @Size(max = 50) String unit) {

    /**
     * Starts the curried construction of an {@link IngredientDto} (see docs/prompt/architektur.adoc).
     *
     * @return the first step of the curried factory
     */
    public static NameStep curried() {
        return name -> quantity -> unit -> new IngredientDto(name, quantity, unit);
    }

    /** Step 1 of {@link #curried()}: the ingredient name. */
    @FunctionalInterface
    public interface NameStep {

        /**
         * @param name ingredient name (required)
         * @return the next step
         */
        QuantityStep name(String name);
    }

    /** Step 2 of {@link #curried()}: the quantity. */
    @FunctionalInterface
    public interface QuantityStep {

        /**
         * @param quantity amount (optional, must be &gt;= 0 if present)
         * @return the next step
         */
        UnitStep quantity(BigDecimal quantity);
    }

    /** Step 3 of {@link #curried()}: the unit, completing the DTO. */
    @FunctionalInterface
    public interface UnitStep {

        /**
         * @param unit unit of the quantity (optional)
         * @return the finished {@link IngredientDto}
         */
        IngredientDto unit(String unit);
    }
}
