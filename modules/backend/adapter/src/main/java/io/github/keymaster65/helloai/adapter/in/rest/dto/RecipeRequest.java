package io.github.keymaster65.helloai.adapter.in.rest.dto;

import io.github.keymaster65.helloai.domain.model.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "RecipeRequest", description = "Recipe payload for create and update operations")
public record RecipeRequest(
        @Schema(description = "Title of the recipe", example = "Spaghetti Carbonara")
        @NotBlank @Size(max = 200) String title,

        @Schema(description = "Free-text description", example = "Classic Roman pasta")
        String description,

        @Schema(description = "Number of servings", example = "4")
        @Positive Integer servings,

        @Schema(description = "Preparation time in minutes", example = "25")
        @Positive Integer prepTimeMinutes,

        @Schema(description = "Difficulty level", example = "MEDIUM")
        @NotNull Difficulty difficulty,

        // @Valid belongs on the type argument, not on the container: the latter is deprecated
        // in Bean Validation (HV000271).
        @Schema(description = "Ingredients of the recipe")
        List<@Valid IngredientDto> ingredients,

        @Schema(description = "Preparation steps; their order in this list defines the position")
        List<@Valid PreparationStepDto> steps) {
}
