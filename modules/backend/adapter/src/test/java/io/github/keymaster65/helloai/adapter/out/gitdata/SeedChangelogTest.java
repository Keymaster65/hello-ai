package io.github.keymaster65.helloai.adapter.out.gitdata;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.keymaster65.helloai.domain.model.Difficulty;
import io.github.keymaster65.helloai.domain.model.Ingredient;
import io.github.keymaster65.helloai.domain.model.PreparationStep;
import io.github.keymaster65.helloai.domain.model.Recipe;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Reads the seed changeset of the relational adapter as the git-backed store reads it (ADR 0055).
 *
 * <p>This is the test that makes „the same six recipes" checkable: it asserts against the
 * <em>content</em> of the changeset, so a changed seed shows up here and not first as a difference
 * between two stores.
 */
class SeedChangelogTest {

    private final List<Recipe> recipes = SeedChangelog.recipes();

    @Test
    void shouldReadTheSixRecipesOfTheFastingCycleInOrder() {
        assertThat(recipes).extracting(Recipe::title).containsExactly(
                "Entlastungstag: Reis mit gedünstetem Gemüse",
                "Fastentee-Mischung",
                "Buchinger Fastenbrühe",
                "Fastensuppe mit passiertem Gemüse",
                "Fastenbrechen: Gedünsteter Apfel",
                "Aufbautag: Kartoffel-Gemüse-Suppe");
    }

    @Test
    void shouldReadEveryRowOfTheChangeset() {
        // 6 recipes, 38 ingredients, 27 steps – the rows the changeset inserts.
        assertThat(recipes.stream().mapToInt(recipe -> recipe.ingredients().size()).sum()).isEqualTo(38);
        assertThat(recipes.stream().mapToInt(recipe -> recipe.steps().size()).sum()).isEqualTo(27);
    }

    @Test
    void shouldAssignEveryChildToItsRecipeThroughTheTitleOfTheSubselect() {
        Recipe tea = recipes.get(1);

        assertThat(tea.title()).isEqualTo("Fastentee-Mischung");
        assertThat(tea.ingredients()).extracting(Ingredient::name)
                .allSatisfy(name -> assertThat(name).isNotBlank())
                .hasSizeGreaterThan(1);
        assertThat(tea.steps()).extracting(PreparationStep::position)
                .containsExactlyElementsOf(
                        IntStream.rangeClosed(1, tea.steps().size()).boxed().toList());
    }

    @Test
    void shouldReadTheColumnsOfTheFirstRecipe() {
        Recipe first = recipes.getFirst();

        assertThat(first.servings()).isEqualTo(1);
        assertThat(first.prepTimeMinutes()).isEqualTo(30);
        assertThat(first.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(first.description()).contains("Buchinger-Heilfasten");
        assertThat(first.ingredients().getFirst())
                .isEqualTo(new Ingredient("Vollkornreis", new BigDecimal("60"), "g"));
        assertThat(first.steps().getFirst().instruction())
                .isEqualTo("Reis in der doppelten Menge Wasser 30 Minuten weich garen.");
    }

    @Test
    void shouldKeepTheStepsInTheOrderOfTheirPosition() {
        assertThat(recipes).allSatisfy(recipe ->
                assertThat(recipe.steps()).extracting(PreparationStep::position)
                        .containsExactlyElementsOf(
                                IntStream.rangeClosed(1, recipe.steps().size()).boxed().toList()));
    }
}
