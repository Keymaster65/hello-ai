package io.github.keymaster65.helloai.domain;

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
}
