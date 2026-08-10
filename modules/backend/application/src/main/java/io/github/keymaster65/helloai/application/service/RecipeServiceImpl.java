package io.github.keymaster65.helloai.application.service;

import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.github.keymaster65.helloai.application.port.out.RecipeRepository;
import io.github.keymaster65.helloai.domain.model.Recipe;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementing the recipe use cases. Contains the business rules and
 * delegates persistence to the {@link RecipeRepository} outbound port.
 */
@Service
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeServiceImpl(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    @Transactional
    public Recipe create(Recipe recipe) {
        return recipeRepository.save(recipe);
    }

    @Override
    @Transactional(readOnly = true)
    public Recipe getById(long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recipe> getAll() {
        return recipeRepository.findAll();
    }

    @Override
    @Transactional
    public Recipe update(long id, Recipe recipe) {
        return recipeRepository.update(id, recipe)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    @Override
    @Transactional
    public void delete(long id) {
        if (!recipeRepository.deleteById(id)) {
            throw new RecipeNotFoundException(id);
        }
    }
}
