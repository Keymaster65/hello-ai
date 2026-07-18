package io.github.keymaster65.helloai.adapter.in.rest.dto;

import io.github.keymaster65.helloai.domain.Difficulty;
import java.util.List;

/**
 * API response body representing a persisted recipe.
 *
 * @param id              database identifier
 * @param title           title of the recipe
 * @param description     free-text description
 * @param servings        number of servings
 * @param prepTimeMinutes preparation time in minutes
 * @param difficulty      difficulty level
 * @param ingredients     ingredients
 * @param steps           ordered preparation steps
 */
public record RecipeResponse(
        Long id,
        String title,
        String description,
        Integer servings,
        Integer prepTimeMinutes,
        Difficulty difficulty,
        List<IngredientDto> ingredients,
        List<PreparationStepResponse> steps) {
}
