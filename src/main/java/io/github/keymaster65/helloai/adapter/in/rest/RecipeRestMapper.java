package io.github.keymaster65.helloai.adapter.in.rest;

import io.github.keymaster65.helloai.adapter.in.rest.dto.IngredientDto;
import io.github.keymaster65.helloai.adapter.in.rest.dto.PreparationStepDto;
import io.github.keymaster65.helloai.adapter.in.rest.dto.PreparationStepResponse;
import io.github.keymaster65.helloai.adapter.in.rest.dto.RecipeRequest;
import io.github.keymaster65.helloai.adapter.in.rest.dto.RecipeResponse;
import io.github.keymaster65.helloai.domain.Ingredient;
import io.github.keymaster65.helloai.domain.PreparationStep;
import io.github.keymaster65.helloai.domain.Recipe;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

/**
 * Maps between REST DTOs and the {@link Recipe} domain model, keeping the API decoupled
 * from the domain.
 */
@Component
public class RecipeRestMapper {

    /**
     * Converts an API request into a domain recipe (without an identifier). Step positions
     * are derived from the order of the request list.
     *
     * @param request the API request
     * @return the corresponding domain recipe
     */
    public Recipe toDomain(RecipeRequest request) {
        List<Ingredient> ingredients = Optional.ofNullable(request.ingredients())
                .orElseGet(List::of)
                .stream()
                .map(this::toDomain)
                .toList();

        List<PreparationStepDto> stepDtos = Optional.ofNullable(request.steps()).orElseGet(List::of);
        List<PreparationStep> steps = IntStream.range(0, stepDtos.size())
                .mapToObj(i -> new PreparationStep(i + 1, stepDtos.get(i).instruction()))
                .toList();

        return new Recipe(
                null,
                request.title(),
                request.description(),
                request.servings(),
                request.prepTimeMinutes(),
                request.difficulty(),
                ingredients,
                steps);
    }

    /**
     * Converts a domain recipe into an API response.
     *
     * @param recipe the domain recipe
     * @return the corresponding API response
     */
    public RecipeResponse toResponse(Recipe recipe) {
        List<IngredientDto> ingredients = recipe.ingredients().stream()
                .map(i -> new IngredientDto(i.name(), i.quantity(), i.unit()))
                .toList();

        List<PreparationStepResponse> steps = recipe.steps().stream()
                .map(s -> new PreparationStepResponse(s.position(), s.instruction()))
                .toList();

        return new RecipeResponse(
                recipe.id(),
                recipe.title(),
                recipe.description(),
                recipe.servings(),
                recipe.prepTimeMinutes(),
                recipe.difficulty(),
                ingredients,
                steps);
    }

    private Ingredient toDomain(IngredientDto dto) {
        return new Ingredient(dto.name(), dto.quantity(), dto.unit());
    }
}
