package io.github.keymaster65.helloai.adapter.in.rest.dto;

import io.github.keymaster65.helloai.domain.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * API response body representing a persisted recipe.
 *
 * <p>Fields guaranteed by the domain model are marked {@code REQUIRED} so that generated
 * clients type them as non-optional; only the genuinely nullable ones stay optional
 * (see ADR 0007).
 *
 * @param id              database identifier, always present on a persisted recipe
 * @param title           title of the recipe, never {@code null}
 * @param description     free-text description, may be {@code null}
 * @param servings        number of servings, may be {@code null}
 * @param prepTimeMinutes preparation time in minutes, may be {@code null}
 * @param difficulty      difficulty level, never {@code null}
 * @param ingredients     ingredients, never {@code null} (may be empty)
 * @param steps           ordered preparation steps, never {@code null} (may be empty)
 */
@Schema(name = "RecipeResponse", description = "A persisted recipe")
public record RecipeResponse(
        @Schema(description = "Database identifier", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(description = "Title of the recipe", example = "Spaghetti Carbonara",
                requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(description = "Free-text description", example = "Classic Roman pasta") String description,
        @Schema(description = "Number of servings", example = "4") Integer servings,
        @Schema(description = "Preparation time in minutes", example = "25") Integer prepTimeMinutes,
        @Schema(description = "Difficulty level", example = "MEDIUM",
                requiredMode = Schema.RequiredMode.REQUIRED) Difficulty difficulty,
        @Schema(description = "Ingredients of the recipe",
                requiredMode = Schema.RequiredMode.REQUIRED) List<IngredientDto> ingredients,
        @Schema(description = "Preparation steps in ascending order",
                requiredMode = Schema.RequiredMode.REQUIRED) List<PreparationStepResponse> steps) {
}
