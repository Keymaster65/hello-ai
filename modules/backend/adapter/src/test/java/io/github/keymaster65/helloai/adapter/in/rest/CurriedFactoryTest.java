package io.github.keymaster65.helloai.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.keymaster65.helloai.adapter.in.rest.dto.IngredientDto;
import io.github.keymaster65.helloai.adapter.in.rest.dto.PreparationStepDto;
import io.github.keymaster65.helloai.adapter.in.rest.dto.PreparationStepResponse;
import io.github.keymaster65.helloai.adapter.in.rest.dto.RecipeRequest;
import io.github.keymaster65.helloai.adapter.in.rest.dto.RecipeResponse;
import io.github.keymaster65.helloai.domain.model.Difficulty;
import java.math.BigDecimal;
import java.util.List;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;

/**
 * The curried factories of the API records (ADR 0021) must produce exactly what the canonical
 * constructor produces – the DTOs are still deserialized by Jackson through that constructor.
 */
class CurriedFactoryTest {

    @Property
    void curriedRecipeRequestEqualsCanonicalConstructor(
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String title,
            @ForAll String description,
            @ForAll Integer servings,
            @ForAll Integer prepTimeMinutes,
            @ForAll Difficulty difficulty,
            @ForAll @Size(max = 5) List<@AlphaChars @StringLength(min = 1, max = 20) String> ingredientNames,
            @ForAll @Size(max = 5) List<@AlphaChars @StringLength(min = 1, max = 20) String> instructions) {

        List<IngredientDto> ingredients = ingredientNames.stream()
                .map(name -> new IngredientDto(name, null, null))
                .toList();
        List<PreparationStepDto> steps = instructions.stream()
                .map(PreparationStepDto::new)
                .toList();

        RecipeRequest curried = RecipeRequest.curried()
                .title(title)
                .description(description)
                .servings(servings)
                .prepTimeMinutes(prepTimeMinutes)
                .difficulty(difficulty)
                .ingredients(ingredients)
                .steps(steps);

        assertThat(curried).isEqualTo(new RecipeRequest(
                title, description, servings, prepTimeMinutes, difficulty, ingredients, steps));
    }

    @Property
    void curriedRecipeResponseEqualsCanonicalConstructor(
            @ForAll Long id,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String title,
            @ForAll String description,
            @ForAll Integer servings,
            @ForAll Integer prepTimeMinutes,
            @ForAll Difficulty difficulty) {

        List<IngredientDto> ingredients = List.of(new IngredientDto("Spaghetti", BigDecimal.valueOf(500), "g"));
        List<PreparationStepResponse> steps = List.of(new PreparationStepResponse(1, "Boil the pasta"));

        RecipeResponse curried = RecipeResponse.curried()
                .id(id)
                .title(title)
                .description(description)
                .servings(servings)
                .prepTimeMinutes(prepTimeMinutes)
                .difficulty(difficulty)
                .ingredients(ingredients)
                .steps(steps);

        assertThat(curried).isEqualTo(new RecipeResponse(
                id, title, description, servings, prepTimeMinutes, difficulty, ingredients, steps));
    }

    @Property
    void curriedIngredientDtoEqualsCanonicalConstructor(
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String name,
            @ForAll BigDecimal quantity,
            @ForAll @AlphaChars @StringLength(max = 10) String unit) {

        IngredientDto curried = IngredientDto.curried()
                .name(name)
                .quantity(quantity)
                .unit(unit);

        assertThat(curried).isEqualTo(new IngredientDto(name, quantity, unit));
    }

    @Property
    void curriedErrorResponseEqualsCanonicalConstructor(
            @ForAll int status,
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String error,
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String message) {

        List<ErrorResponse.FieldError> fieldErrors =
                List.of(new ErrorResponse.FieldError("title", "must not be blank"));

        ErrorResponse curried = ErrorResponse.curried()
                .status(status)
                .error(error)
                .message(message)
                .fieldErrors(fieldErrors);

        assertThat(curried).isEqualTo(new ErrorResponse(status, error, message, fieldErrors));
    }

    /** The existing shortcut stays the short form of the full chain. */
    @Example
    void ofIsTheCurriedChainWithoutFieldErrors() {
        ErrorResponse shortcut = ErrorResponse.of(404, "NOT_FOUND", "Recipe 42 not found");

        ErrorResponse curried = ErrorResponse.curried()
                .status(404)
                .error("NOT_FOUND")
                .message("Recipe 42 not found")
                .fieldErrors(List.of());

        assertThat(curried).isEqualTo(shortcut);
    }
}
