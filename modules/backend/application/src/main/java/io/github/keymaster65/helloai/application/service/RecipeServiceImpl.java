package io.github.keymaster65.helloai.application.service;

import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.github.keymaster65.helloai.application.port.out.RecipeRepository;
import io.github.keymaster65.helloai.domain.model.Recipe;
import java.util.List;

/**
 * Application service implementing the recipe use cases. Contains the business rules and
 * delegates persistence to the {@link RecipeRepository} outbound port.
 *
 * <p>Plain Java on purpose: this layer carries no framework annotations at all (ADR 0045).
 * Who instantiates this class, and inside which transaction its use cases run, is decided in
 * the composition root {@code :bootstrap} &ndash; not here.
 */
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeServiceImpl(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    public Recipe create(Recipe recipe) {
        return recipeRepository.save(recipe);
    }

    @Override
    public Recipe getById(long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    @Override
    public List<Recipe> getAll() {
        return recipeRepository.findAll();
    }

    @Override
    public Recipe update(long id, Recipe recipe) {
        return recipeRepository.update(id, recipe)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    @Override
    public void delete(long id) {
        if (!recipeRepository.deleteById(id)) {
            throw new RecipeNotFoundException(id);
        }
    }
}
