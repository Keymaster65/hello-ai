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
}
