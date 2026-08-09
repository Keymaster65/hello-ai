package io.github.keymaster65.helloai.adapter.in.rest.dto;

import io.github.keymaster65.helloai.domain.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "RecipeResponse", description = "A persisted recipe")
public record RecipeResponse(
        @Schema(description = "Database identifier", example = "1") Long id,
        @Schema(description = "Title of the recipe", example = "Spaghetti Carbonara") String title,
        @Schema(description = "Free-text description", example = "Classic Roman pasta") String description,
        @Schema(description = "Number of servings", example = "4") Integer servings,
        @Schema(description = "Preparation time in minutes", example = "25") Integer prepTimeMinutes,
        @Schema(description = "Difficulty level", example = "MEDIUM") Difficulty difficulty,
        @Schema(description = "Ingredients of the recipe") List<IngredientDto> ingredients,
        @Schema(description = "Preparation steps in ascending order") List<PreparationStepResponse> steps) {
}
