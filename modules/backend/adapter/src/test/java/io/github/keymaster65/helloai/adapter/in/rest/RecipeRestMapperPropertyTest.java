package io.github.keymaster65.helloai.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.keymaster65.helloai.adapter.in.rest.dto.IngredientDto;
import io.github.keymaster65.helloai.adapter.in.rest.dto.PreparationStepDto;
import io.github.keymaster65.helloai.adapter.in.rest.dto.RecipeRequest;
import io.github.keymaster65.helloai.domain.Difficulty;
import io.github.keymaster65.helloai.domain.Recipe;
import java.util.List;
import java.util.stream.IntStream;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;

/**
 * Property-based tests (jqwik) for the REST mapper's structural invariants.
 */
class RecipeRestMapperPropertyTest {

    private final RecipeRestMapper mapper = new RecipeRestMapper();

    @Property
    void stepPositionsAreAlwaysSequentialAndOneBased(
            @ForAll @Size(min = 1, max = 20)
            List<@AlphaChars @StringLength(min = 1, max = 30) String> instructions) {

        List<PreparationStepDto> stepDtos = instructions.stream()
                .map(PreparationStepDto::new)
                .toList();
        RecipeRequest request =
                new RecipeRequest("Title", null, null, null, Difficulty.EASY, List.of(), stepDtos);

        Recipe recipe = mapper.toDomain(request);

        List<Integer> expected = IntStream.rangeClosed(1, instructions.size()).boxed().toList();
        assertThat(recipe.steps()).extracting("position").containsExactlyElementsOf(expected);
        assertThat(recipe.steps()).extracting("instruction").containsExactlyElementsOf(instructions);
    }

    @Property
    void ingredientsArePreservedInOrder(
            @ForAll @Size(max = 20)
            List<@AlphaChars @StringLength(min = 1, max = 30) String> names) {

        List<IngredientDto> ingredientDtos = names.stream()
                .map(name -> new IngredientDto(name, null, null))
                .toList();
        RecipeRequest request =
                new RecipeRequest("Title", null, null, null, Difficulty.MEDIUM, ingredientDtos, List.of());

        Recipe recipe = mapper.toDomain(request);

        assertThat(recipe.ingredients()).extracting("name").containsExactlyElementsOf(names);
    }
}
