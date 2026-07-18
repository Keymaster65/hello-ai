package io.github.keymaster65.helloai.adapter.in.rest.dto;

import io.github.keymaster65.helloai.domain.Difficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * API request body used for creating and updating a recipe.
 *
 * @param title           title of the recipe (required)
 * @param description     free-text description (optional)
 * @param servings        number of servings (optional, must be positive if present)
 * @param prepTimeMinutes preparation time in minutes (optional, must be positive if present)
 * @param difficulty      difficulty level (required)
 * @param ingredients     ingredients (optional, each validated)
 * @param steps           ordered preparation steps (optional, each validated)
 */
public record RecipeRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @Positive Integer servings,
        @Positive Integer prepTimeMinutes,
        @NotNull Difficulty difficulty,
        @Valid List<IngredientDto> ingredients,
        @Valid List<PreparationStepDto> steps) {
}
