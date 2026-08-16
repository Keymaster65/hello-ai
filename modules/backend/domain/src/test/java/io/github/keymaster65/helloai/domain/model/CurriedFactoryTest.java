package io.github.keymaster65.helloai.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;

/**
 * The curried factories of the domain records (docs/prompt/architektur.adoc) are an alternative way to reach the
 * canonical constructor – not a second construction path with rules of its own. These tests pin
 * exactly that: same result, same validation.
 */
class CurriedFactoryTest {

    @Property
    void curriedRecipeEqualsCanonicalConstructor(
            @ForAll Long id,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String title,
            @ForAll String description,
            @ForAll Integer servings,
            @ForAll Integer prepTimeMinutes,
            @ForAll Difficulty difficulty,
            @ForAll @Size(max = 5) List<@AlphaChars @StringLength(min = 1, max = 20) String> ingredientNames,
            @ForAll @Size(max = 5) List<@AlphaChars @StringLength(min = 1, max = 20) String> instructions) {

        List<Ingredient> ingredients = ingredientNames.stream()
                .map(name -> new Ingredient(name, null, null))
                .toList();
        List<PreparationStep> steps = IntStream.rangeClosed(1, instructions.size())
                .mapToObj(position -> new PreparationStep(position, instructions.get(position - 1)))
                .toList();

        Recipe curried = Recipe.curried()
                .id(id)
                .title(title)
                .description(description)
                .servings(servings)
                .prepTimeMinutes(prepTimeMinutes)
                .difficulty(difficulty)
                .ingredients(ingredients)
                .steps(steps);

        assertThat(curried).isEqualTo(new Recipe(
                id, title, description, servings, prepTimeMinutes, difficulty, ingredients, steps));
    }

    @Property
    void curriedIngredientEqualsCanonicalConstructor(
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String name,
            @ForAll BigDecimal quantity,
            @ForAll @AlphaChars @StringLength(max = 10) String unit) {

        Ingredient curried = Ingredient.curried()
                .name(name)
                .quantity(quantity)
                .unit(unit);

        assertThat(curried).isEqualTo(new Ingredient(name, quantity, unit));
    }

    /**
     * The steps must not become a way around the compact constructor: validation happens when the
     * last step assembles the record, not earlier and not never.
     */
    @Example
    void curriedRecipeRejectsABlankTitle() {
        Recipe.TitleStep afterId = Recipe.curried().id(1L);

        assertThatThrownBy(() -> afterId
                .title("  ")
                .description(null)
                .servings(null)
                .prepTimeMinutes(null)
                .difficulty(Difficulty.EASY)
                .ingredients(List.of())
                .steps(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Example
    void curriedIngredientRejectsANullName() {
        Ingredient.NameStep start = Ingredient.curried();

        assertThatThrownBy(() -> start.name(null).quantity(BigDecimal.ONE).unit("g"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");
    }

    /**
     * Currying means the chain can be stopped and reused: the steps up to the difficulty are a
     * partially applied factory that several recipes share.
     */
    @Example
    void aPartiallyAppliedChainCanBeReused() {
        Recipe.IngredientsStep base = Recipe.curried()
                .id(null)
                .title("Spaghetti Carbonara")
                .description("Classic Roman pasta")
                .servings(4)
                .prepTimeMinutes(25)
                .difficulty(Difficulty.MEDIUM);

        Recipe withoutIngredients = base.ingredients(List.of()).steps(List.of());
        Recipe withIngredients = base
                .ingredients(List.of(new Ingredient("Spaghetti", BigDecimal.valueOf(500), "g")))
                .steps(List.of());

        assertThat(withoutIngredients.ingredients()).isEmpty();
        assertThat(withIngredients.ingredients()).hasSize(1);
        assertThat(withIngredients.title()).isEqualTo(withoutIngredients.title());
    }
}
