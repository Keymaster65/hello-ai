package io.github.keymaster65.helloai.application.port.in;

import io.github.keymaster65.helloai.domain.model.Recipe;
import java.util.List;

/**
 * Inbound port: use cases for managing recipes. Implemented by the application layer
 * and driven by inbound adapters (e.g. the REST controller).
 */
public interface RecipeService {

    /**
     * Creates and persists a new recipe.
     *
     * @param recipe the recipe to create (its {@code id} is ignored)
     * @return the persisted recipe including its generated identifier
     */
    Recipe create(Recipe recipe);

    /**
     * Returns the recipe with the given identifier.
     *
     * @param id the identifier
     * @return the recipe
     * @throws io.github.keymaster65.helloai.application.service.RecipeNotFoundException
     *         if no recipe with {@code id} exists
     */
    Recipe getById(long id);

    /**
     * Returns all recipes.
     *
     * @return all recipes, possibly empty
     */
    List<Recipe> getAll();

    /**
     * Replaces the recipe with the given identifier.
     *
     * @param id     the identifier of the recipe to update
     * @param recipe the new recipe state (its {@code id} is ignored)
     * @return the updated recipe
     * @throws io.github.keymaster65.helloai.application.service.RecipeNotFoundException
     *         if no recipe with {@code id} exists
     */
    Recipe update(long id, Recipe recipe);

    /**
     * Deletes the recipe with the given identifier.
     *
     * @param id the identifier
     * @throws io.github.keymaster65.helloai.application.service.RecipeNotFoundException
     *         if no recipe with {@code id} exists
     */
    void delete(long id);
}
