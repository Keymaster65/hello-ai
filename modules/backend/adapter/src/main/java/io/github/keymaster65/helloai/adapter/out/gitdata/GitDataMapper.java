package io.github.keymaster65.helloai.adapter.out.gitdata;

import io.github.keymaster65.helloai.domain.model.Difficulty;
import io.github.keymaster65.helloai.domain.model.Ingredient;
import io.github.keymaster65.helloai.domain.model.PreparationStep;
import io.github.keymaster65.helloai.domain.model.Recipe;
import java.time.Instant;
import java.util.List;

/**
 * Maps between the {@link Recipe} domain model and the stored documents (ADR 0053).
 *
 * <p>The counterpart of {@code RecipePersistenceMapper} for the other adapter: it keeps the file
 * format out of the domain and the domain out of the file format. Identifiers and positions are
 * <em>given</em> to this class &ndash; assigning them is the repository's job, because only it sees
 * what is already stored.
 */
final class GitDataMapper {

    /**
     * @param id        identifier to store with the recipe
     * @param recipe    the recipe to store; its own {@code id} is ignored
     * @param createdAt when the row was first written &ndash; kept across a replacement
     * @param updatedAt when it was written now
     * @return the document to write
     */
    RecipeDocument toDocument(long id, Recipe recipe, Instant createdAt, Instant updatedAt) {
        return RecipeDocument.curried()
                .id(id)
                .title(recipe.title())
                .description(recipe.description())
                .servings(recipe.servings())
                .prepTimeMinutes(recipe.prepTimeMinutes())
                .difficulty(recipe.difficulty().name())
                .createdAt(createdAt)
                .updatedAt(updatedAt);
    }

    /**
     * @param id         identifier to store with the ingredient
     * @param recipeId   identifier of the owning recipe
     * @param position   1-based order within that recipe
     * @param ingredient the ingredient to store
     * @return the document to write
     */
    IngredientDocument toDocument(long id, long recipeId, int position, Ingredient ingredient) {
        return IngredientDocument.curried()
                .id(id)
                .recipeId(recipeId)
                .position(position)
                .name(ingredient.name())
                .quantity(ingredient.quantity())
                .unit(ingredient.unit());
    }

    /**
     * @param id       identifier to store with the step
     * @param recipeId identifier of the owning recipe
     * @param step     the step to store; it carries its own position
     * @return the document to write
     */
    PreparationStepDocument toDocument(long id, long recipeId, PreparationStep step) {
        return PreparationStepDocument.curried()
                .id(id)
                .recipeId(recipeId)
                .position(step.position())
                .instruction(step.instruction());
    }

    /**
     * @param recipe      the stored recipe
     * @param ingredients its ingredients, already ordered by position
     * @param steps       its preparation steps, already ordered by position
     * @return the recipe as the domain sees it
     */
    Recipe toDomain(
            RecipeDocument recipe,
            List<IngredientDocument> ingredients,
            List<PreparationStepDocument> steps) {
        return Recipe.curried()
                .id(recipe.id())
                .title(recipe.title())
                .description(recipe.description())
                .servings(recipe.servings())
                .prepTimeMinutes(recipe.prepTimeMinutes())
                // Same behaviour as the jOOQ adapter: an unknown name is a broken store, and it
                // fails loudly instead of turning into a default difficulty.
                .difficulty(Difficulty.valueOf(recipe.difficulty()))
                .ingredients(ingredients.stream()
                        .map(i -> new Ingredient(i.name(), i.quantity(), i.unit()))
                        .toList())
                .steps(steps.stream()
                        .map(s -> new PreparationStep(s.position(), s.instruction()))
                        .toList());
    }
}
