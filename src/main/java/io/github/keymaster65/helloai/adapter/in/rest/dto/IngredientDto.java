package io.github.keymaster65.helloai.adapter.in.rest.dto;

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
public record IngredientDto(
        @NotBlank @Size(max = 200) String name,
        @PositiveOrZero BigDecimal quantity,
        @Size(max = 50) String unit) {
}
