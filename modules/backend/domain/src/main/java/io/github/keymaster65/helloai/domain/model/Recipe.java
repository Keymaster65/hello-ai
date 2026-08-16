package io.github.keymaster65.helloai.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Aggregate root of the domain: a recipe with its ingredients and preparation steps.
 *
 * <p>Instances are immutable. A {@code null} {@link #id()} marks a recipe that has not
 * been persisted yet.
 *
 * @param id              database identifier, {@code null} for a not-yet-persisted recipe
 * @param title           title of the recipe, must not be {@code null} or blank
 * @param description     free-text description, may be {@code null}
 * @param servings        number of servings, may be {@code null}
 * @param prepTimeMinutes preparation time in minutes, may be {@code null}
 * @param difficulty      difficulty level, must not be {@code null}
 * @param ingredients     ingredients, never {@code null} (defaults to an empty list)
 * @param steps           ordered preparation steps, never {@code null} (defaults to an empty list)
 */
public record Recipe(
        Long id,
        String title,
        String description,
        Integer servings,
        Integer prepTimeMinutes,
        Difficulty difficulty,
        List<Ingredient> ingredients,
        List<PreparationStep> steps) {

    public Recipe {
        Objects.requireNonNull(title, "title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        Objects.requireNonNull(difficulty, "difficulty must not be null");
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    /**
     * Returns a copy of this recipe with the given identifier assigned.
     *
     * @param newId the identifier to assign
     * @return a new {@link Recipe} instance carrying {@code newId}
     */
    public Recipe withId(Long newId) {
        return new Recipe(newId, title, description, servings, prepTimeMinutes, difficulty, ingredients, steps);
    }

    /**
     * Starts the curried construction of a {@link Recipe} (see docs/prompt/architektur.adoc).
     *
     * <p>Every component is supplied by its own named step. With four adjacent components of
     * only two types ({@code String}/{@code Integer}), the canonical constructor accepts a
     * swapped pair silently; the steps do not:
     *
     * <pre>{@code
     * Recipe recipe = Recipe.curried()
     *         .id(null)
     *         .title("Spaghetti Carbonara")
     *         .description("Classic Roman pasta")
     *         .servings(4)
     *         .prepTimeMinutes(25)
     *         .difficulty(Difficulty.MEDIUM)
     *         .ingredients(List.of())
     *         .steps(List.of());
     * }</pre>
     *
     * @return the first step of the curried factory
     */
    public static IdStep curried() {
        return id -> title -> description -> servings -> prepTimeMinutes -> difficulty -> ingredients -> steps ->
                new Recipe(id, title, description, servings, prepTimeMinutes, difficulty, ingredients, steps);
    }

    /** Step 1 of {@link #curried()}: the identifier. */
    @FunctionalInterface
    public interface IdStep {

        /**
         * @param id database identifier, {@code null} for a not-yet-persisted recipe
         * @return the next step
         */
        TitleStep id(Long id);
    }

    /** Step 2 of {@link #curried()}: the title. */
    @FunctionalInterface
    public interface TitleStep {

        /**
         * @param title title of the recipe, must not be {@code null} or blank
         * @return the next step
         */
        DescriptionStep title(String title);
    }

    /** Step 3 of {@link #curried()}: the description. */
    @FunctionalInterface
    public interface DescriptionStep {

        /**
         * @param description free-text description, may be {@code null}
         * @return the next step
         */
        ServingsStep description(String description);
    }

    /** Step 4 of {@link #curried()}: the number of servings. */
    @FunctionalInterface
    public interface ServingsStep {

        /**
         * @param servings number of servings, may be {@code null}
         * @return the next step
         */
        PrepTimeMinutesStep servings(Integer servings);
    }

    /** Step 5 of {@link #curried()}: the preparation time. */
    @FunctionalInterface
    public interface PrepTimeMinutesStep {

        /**
         * @param prepTimeMinutes preparation time in minutes, may be {@code null}
         * @return the next step
         */
        DifficultyStep prepTimeMinutes(Integer prepTimeMinutes);
    }

    /** Step 6 of {@link #curried()}: the difficulty. */
    @FunctionalInterface
    public interface DifficultyStep {

        /**
         * @param difficulty difficulty level, must not be {@code null}
         * @return the next step
         */
        IngredientsStep difficulty(Difficulty difficulty);
    }

    /** Step 7 of {@link #curried()}: the ingredients. */
    @FunctionalInterface
    public interface IngredientsStep {

        /**
         * @param ingredients ingredients, {@code null} is treated as an empty list
         * @return the next step
         */
        StepsStep ingredients(List<Ingredient> ingredients);
    }

    /** Step 8 of {@link #curried()}: the preparation steps, completing the recipe. */
    @FunctionalInterface
    public interface StepsStep {

        /**
         * @param steps ordered preparation steps, {@code null} is treated as an empty list
         * @return the finished {@link Recipe}
         */
        Recipe steps(List<PreparationStep> steps);
    }
}
