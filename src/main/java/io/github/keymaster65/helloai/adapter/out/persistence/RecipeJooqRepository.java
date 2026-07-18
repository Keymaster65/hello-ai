package io.github.keymaster65.helloai.adapter.out.persistence;

import static io.github.keymaster65.helloai.adapter.out.persistence.jooq.Tables.INGREDIENT;
import static io.github.keymaster65.helloai.adapter.out.persistence.jooq.Tables.PREPARATION_STEP;
import static io.github.keymaster65.helloai.adapter.out.persistence.jooq.Tables.RECIPE;

import io.github.keymaster65.helloai.adapter.out.persistence.jooq.tables.records.IngredientRecord;
import io.github.keymaster65.helloai.adapter.out.persistence.jooq.tables.records.PreparationStepRecord;
import io.github.keymaster65.helloai.adapter.out.persistence.jooq.tables.records.RecipeRecord;
import io.github.keymaster65.helloai.application.port.out.RecipeRepository;
import io.github.keymaster65.helloai.domain.Ingredient;
import io.github.keymaster65.helloai.domain.PreparationStep;
import io.github.keymaster65.helloai.domain.Recipe;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * jOOQ-based implementation of the {@link RecipeRepository} outbound port. Persists a
 * recipe together with its ordered ingredients and preparation steps.
 */
@Repository
class RecipeJooqRepository implements RecipeRepository {

    private final DSLContext dsl;
    private final RecipePersistenceMapper mapper;

    RecipeJooqRepository(DSLContext dsl, RecipePersistenceMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
    }

    @Override
    public Recipe save(Recipe recipe) {
        long id = dsl.insertInto(RECIPE)
                .set(RECIPE.TITLE, recipe.title())
                .set(RECIPE.DESCRIPTION, recipe.description())
                .set(RECIPE.SERVINGS, recipe.servings())
                .set(RECIPE.PREP_TIME_MINUTES, recipe.prepTimeMinutes())
                .set(RECIPE.DIFFICULTY, recipe.difficulty().name())
                .returningResult(RECIPE.ID)
                .fetchOne(RECIPE.ID);

        insertChildren(id, recipe);
        return findById(id).orElseThrow();
    }

    @Override
    public Optional<Recipe> findById(long id) {
        RecipeRecord record = dsl.selectFrom(RECIPE).where(RECIPE.ID.eq(id)).fetchOne();
        if (record == null) {
            return Optional.empty();
        }
        List<IngredientRecord> ingredients = dsl.selectFrom(INGREDIENT)
                .where(INGREDIENT.RECIPE_ID.eq(id))
                .orderBy(INGREDIENT.POSITION)
                .fetch();
        List<PreparationStepRecord> steps = dsl.selectFrom(PREPARATION_STEP)
                .where(PREPARATION_STEP.RECIPE_ID.eq(id))
                .orderBy(PREPARATION_STEP.POSITION)
                .fetch();
        return Optional.of(mapper.toDomain(record, ingredients, steps));
    }

    @Override
    public List<Recipe> findAll() {
        List<RecipeRecord> recipes = dsl.selectFrom(RECIPE).orderBy(RECIPE.ID).fetch();
        if (recipes.isEmpty()) {
            return List.of();
        }
        List<Long> ids = recipes.stream().map(RecipeRecord::getId).toList();

        Map<Long, List<IngredientRecord>> ingredientsByRecipe = dsl.selectFrom(INGREDIENT)
                .where(INGREDIENT.RECIPE_ID.in(ids))
                .orderBy(INGREDIENT.RECIPE_ID, INGREDIENT.POSITION)
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(IngredientRecord::getRecipeId));

        Map<Long, List<PreparationStepRecord>> stepsByRecipe = dsl.selectFrom(PREPARATION_STEP)
                .where(PREPARATION_STEP.RECIPE_ID.in(ids))
                .orderBy(PREPARATION_STEP.RECIPE_ID, PREPARATION_STEP.POSITION)
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(PreparationStepRecord::getRecipeId));

        return recipes.stream()
                .map(r -> mapper.toDomain(
                        r,
                        ingredientsByRecipe.getOrDefault(r.getId(), List.of()),
                        stepsByRecipe.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    @Override
    public Optional<Recipe> update(long id, Recipe recipe) {
        int updated = dsl.update(RECIPE)
                .set(RECIPE.TITLE, recipe.title())
                .set(RECIPE.DESCRIPTION, recipe.description())
                .set(RECIPE.SERVINGS, recipe.servings())
                .set(RECIPE.PREP_TIME_MINUTES, recipe.prepTimeMinutes())
                .set(RECIPE.DIFFICULTY, recipe.difficulty().name())
                .set(RECIPE.UPDATED_AT, DSL.currentLocalDateTime())
                .where(RECIPE.ID.eq(id))
                .execute();
        if (updated == 0) {
            return Optional.empty();
        }
        // Replace children wholesale to keep positions consistent with the incoming order.
        dsl.deleteFrom(INGREDIENT).where(INGREDIENT.RECIPE_ID.eq(id)).execute();
        dsl.deleteFrom(PREPARATION_STEP).where(PREPARATION_STEP.RECIPE_ID.eq(id)).execute();
        insertChildren(id, recipe);
        return findById(id);
    }

    @Override
    public boolean deleteById(long id) {
        // Ingredients and steps are removed via ON DELETE CASCADE.
        return dsl.deleteFrom(RECIPE).where(RECIPE.ID.eq(id)).execute() > 0;
    }

    private void insertChildren(long recipeId, Recipe recipe) {
        List<Ingredient> ingredients = recipe.ingredients();
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            dsl.insertInto(INGREDIENT)
                    .set(INGREDIENT.RECIPE_ID, recipeId)
                    .set(INGREDIENT.POSITION, i + 1)
                    .set(INGREDIENT.NAME, ingredient.name())
                    .set(INGREDIENT.QUANTITY, ingredient.quantity())
                    .set(INGREDIENT.UNIT, ingredient.unit())
                    .execute();
        }
        for (PreparationStep step : recipe.steps()) {
            dsl.insertInto(PREPARATION_STEP)
                    .set(PREPARATION_STEP.RECIPE_ID, recipeId)
                    .set(PREPARATION_STEP.POSITION, step.position())
                    .set(PREPARATION_STEP.INSTRUCTION, step.instruction())
                    .execute();
        }
    }
}
