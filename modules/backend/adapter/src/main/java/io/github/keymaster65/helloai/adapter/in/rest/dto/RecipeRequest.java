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

    /**
     * Starts the curried construction of a {@link RecipeRequest} (see docs/prompt/architektur.adoc).
     *
     * <p>Jackson keeps using the canonical constructor when deserializing a request body; the
     * steps exist for the code that builds a request by hand, above all the tests.
     *
     * @return the first step of the curried factory
     */
    public static TitleStep curried() {
        return title -> description -> servings -> prepTimeMinutes -> difficulty -> ingredients -> steps ->
                new RecipeRequest(title, description, servings, prepTimeMinutes, difficulty, ingredients, steps);
    }

    /** Step 1 of {@link #curried()}: the title. */
    @FunctionalInterface
    public interface TitleStep {

        /**
         * @param title title of the recipe (required)
         * @return the next step
         */
        DescriptionStep title(String title);
    }

    /** Step 2 of {@link #curried()}: the description. */
    @FunctionalInterface
    public interface DescriptionStep {

        /**
         * @param description free-text description (optional)
         * @return the next step
         */
        ServingsStep description(String description);
    }

    /** Step 3 of {@link #curried()}: the number of servings. */
    @FunctionalInterface
    public interface ServingsStep {

        /**
         * @param servings number of servings (optional, must be positive if present)
         * @return the next step
         */
        PrepTimeMinutesStep servings(Integer servings);
    }

    /** Step 4 of {@link #curried()}: the preparation time. */
    @FunctionalInterface
    public interface PrepTimeMinutesStep {

        /**
         * @param prepTimeMinutes preparation time in minutes (optional, must be positive if present)
         * @return the next step
         */
        DifficultyStep prepTimeMinutes(Integer prepTimeMinutes);
    }

    /** Step 5 of {@link #curried()}: the difficulty. */
    @FunctionalInterface
    public interface DifficultyStep {

        /**
         * @param difficulty difficulty level (required)
         * @return the next step
         */
        IngredientsStep difficulty(Difficulty difficulty);
    }

    /** Step 6 of {@link #curried()}: the ingredients. */
    @FunctionalInterface
    public interface IngredientsStep {

        /**
         * @param ingredients ingredients (optional, each validated)
         * @return the next step
         */
        StepsStep ingredients(List<IngredientDto> ingredients);
    }

    /** Step 7 of {@link #curried()}: the preparation steps, completing the request. */
    @FunctionalInterface
    public interface StepsStep {

        /**
         * @param steps ordered preparation steps (optional, each validated)
         * @return the finished {@link RecipeRequest}
         */
        RecipeRequest steps(List<PreparationStepDto> steps);
    }
}
