package io.github.keymaster65.helloai.adapter.out.persistence;

import io.github.keymaster65.helloai.adapter.out.persistence.jooq.tables.records.IngredientRecord;
import io.github.keymaster65.helloai.adapter.out.persistence.jooq.tables.records.PreparationStepRecord;
import io.github.keymaster65.helloai.adapter.out.persistence.jooq.tables.records.RecipeRecord;
import io.github.keymaster65.helloai.domain.model.Difficulty;
import io.github.keymaster65.helloai.domain.model.Ingredient;
import io.github.keymaster65.helloai.domain.model.PreparationStep;
import io.github.keymaster65.helloai.domain.model.Recipe;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Maps jOOQ records to the {@link Recipe} domain model.
 */
@Component
class RecipePersistenceMapper {

    Recipe toDomain(
            RecipeRecord recipe,
            List<IngredientRecord> ingredients,
            List<PreparationStepRecord> steps) {
        List<Ingredient> mappedIngredients = ingredients.stream()
                .map(i -> new Ingredient(i.getName(), i.getQuantity(), i.getUnit()))
                .toList();
        List<PreparationStep> mappedSteps = steps.stream()
                .map(s -> new PreparationStep(s.getPosition(), s.getInstruction()))
                .toList();
        return new Recipe(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getServings(),
                recipe.getPrepTimeMinutes(),
                Difficulty.valueOf(recipe.getDifficulty()),
                mappedIngredients,
                mappedSteps);
    }
}
