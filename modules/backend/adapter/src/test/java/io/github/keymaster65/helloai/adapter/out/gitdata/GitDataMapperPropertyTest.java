package io.github.keymaster65.helloai.adapter.out.gitdata;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.keymaster65.helloai.domain.model.Difficulty;
import io.github.keymaster65.helloai.domain.model.Ingredient;
import io.github.keymaster65.helloai.domain.model.PreparationStep;
import io.github.keymaster65.helloai.domain.model.Recipe;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Property-based tests (jqwik) for the mapping and the stored format (ADR 0053).
 *
 * <p>The interesting invariant of a persistence format is the round trip: whatever goes in comes
 * back unchanged. Checked here across the mapper <em>and</em> Jackson, because a format that only
 * survives the mapper would still lose data in the file.
 */
class GitDataMapperPropertyTest {

    private final GitDataMapper mapper = new GitDataMapper();
    private final ObjectMapper json = JsonMapper.builder().build();

    @Property
    void aRecipeSurvivesTheRoundTripThroughDocumentsAndJson(
            @ForAll @AlphaChars @StringLength(min = 1, max = 40) String title,
            @ForAll @IntRange(min = 1, max = 99) int servings,
            @ForAll Difficulty difficulty,
            @ForAll @Size(max = 8) List<@AlphaChars @StringLength(min = 1, max = 20) String> names) {

        Recipe original = Recipe.curried()
                .id(null)
                .title(title)
                .description(null)
                .servings(servings)
                .prepTimeMinutes(null)
                .difficulty(difficulty)
                .ingredients(names.stream()
                        .map(name -> new Ingredient(name, BigDecimal.valueOf(2, 1), "g"))
                        .toList())
                .steps(List.of(new PreparationStep(1, "Kochen")));

        Instant written = Instant.parse("2026-08-17T12:00:00Z");
        RecipeDocument recipe =
                throughJson(mapper.toDocument(7L, original, written, written), RecipeDocument.class);
        // The timestamps are part of the format since ADR 0055 and have to survive the file, too.
        assertThat(recipe.createdAt()).isEqualTo(written);
        assertThat(recipe.updatedAt()).isEqualTo(written);
        List<IngredientDocument> ingredients = IntStream.range(0, names.size())
                .mapToObj(index -> mapper.toDocument(
                        index + 1L, 7L, index + 1, original.ingredients().get(index)))
                .map(document -> throughJson(document, IngredientDocument.class))
                .toList();
        List<PreparationStepDocument> steps = original.steps().stream()
                .map(step -> mapper.toDocument(1L, 7L, step))
                .map(document -> throughJson(document, PreparationStepDocument.class))
                .toList();

        Recipe restored = mapper.toDomain(recipe, ingredients, steps);

        assertThat(restored).isEqualTo(original.withId(7L));
    }

    @Property
    void everyChildKnowsItsRecipeAndItsPosition(
            @ForAll @Size(min = 1, max = 10) List<@AlphaChars @StringLength(min = 1, max = 20) String> names) {

        List<IngredientDocument> documents = IntStream.range(0, names.size())
                .mapToObj(index -> mapper.toDocument(
                        index + 1L, 42L, index + 1, new Ingredient(names.get(index), null, null)))
                .toList();

        assertThat(documents).allSatisfy(document ->
                assertThat(document.recipeId()).isEqualTo(42L));
        assertThat(documents).extracting(IngredientDocument::position)
                .containsExactlyElementsOf(IntStream.rangeClosed(1, names.size()).boxed().toList());
    }

    private <T> T throughJson(T document, Class<T> type) {
        return json.readValue(json.writeValueAsBytes(document), type);
    }
}
