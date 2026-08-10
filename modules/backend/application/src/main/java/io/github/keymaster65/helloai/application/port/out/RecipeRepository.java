package io.github.keymaster65.helloai.application.port.out;

import io.github.keymaster65.helloai.domain.model.Recipe;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port: persistence operations for {@link Recipe} aggregates.
 * Implemented by an outbound adapter (e.g. the jOOQ repository).
 */
public interface RecipeRepository {

    /**
     * Persists a new recipe.
     *
     * @param recipe the recipe to persist (its {@code id} is ignored)
     * @return the persisted recipe including its generated identifier
     */
    Recipe save(Recipe recipe);

    /**
     * Finds a recipe by its identifier.
     *
     * @param id the identifier
     * @return the recipe, or {@link Optional#empty()} if none exists
     */
    Optional<Recipe> findById(long id);

    /**
     * Returns all recipes.
     *
     * @return all recipes, possibly empty
     */
    List<Recipe> findAll();

    /**
     * Replaces an existing recipe.
     *
     * @param id     the identifier of the recipe to update
     * @param recipe the new recipe state
     * @return the updated recipe, or {@link Optional#empty()} if none exists
     */
    Optional<Recipe> update(long id, Recipe recipe);

    /**
     * Deletes a recipe by its identifier.
     *
     * @param id the identifier
     * @return {@code true} if a recipe was deleted, {@code false} if none existed
     */
    boolean deleteById(long id);
}
