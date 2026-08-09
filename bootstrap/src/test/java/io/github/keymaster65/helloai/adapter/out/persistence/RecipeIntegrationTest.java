package io.github.keymaster65.helloai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.github.keymaster65.helloai.application.service.RecipeNotFoundException;
import io.github.keymaster65.helloai.bootstrap.RecipeApplication;
import io.github.keymaster65.helloai.domain.Difficulty;
import io.github.keymaster65.helloai.domain.Ingredient;
import io.github.keymaster65.helloai.domain.PreparationStep;
import io.github.keymaster65.helloai.domain.Recipe;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Full-stack persistence integration test: real PostgreSQL (embedded, no Docker), Liquibase
 * migrations and the jOOQ repository via the {@link RecipeService}.
 *
 * <p>In an environment with Docker this would typically use Testcontainers; here the
 * Docker-free embedded PostgreSQL is used instead. If the native binary cannot start in
 * the current sandbox, the whole test class is skipped (see {@link #embeddedPostgresAvailable()}).
 */
@SpringBootTest(classes = RecipeApplication.class)
@EnabledIf("embeddedPostgresAvailable")
class RecipeIntegrationTest {

    private static EmbeddedPostgres embeddedPostgres;

    static {
        try {
            embeddedPostgres = EmbeddedPostgres.builder().start();
        } catch (Throwable _) {
            embeddedPostgres = null;
        }
    }

    static boolean embeddedPostgresAvailable() {
        return embeddedPostgres != null;
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> embeddedPostgres.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void stopEmbeddedPostgres() throws Exception {
        if (embeddedPostgres != null) {
            embeddedPostgres.close();
        }
    }

    @Autowired
    private RecipeService recipeService;

    private Recipe newRecipe() {
        return new Recipe(
                null,
                "Spaghetti Carbonara",
                "Classic Roman pasta",
                4,
                25,
                Difficulty.MEDIUM,
                List.of(
                        new Ingredient("Spaghetti", new BigDecimal("500"), "g"),
                        new Ingredient("Eggs", new BigDecimal("4"), "pcs")),
                List.of(
                        new PreparationStep(1, "Boil the pasta"),
                        new PreparationStep(2, "Mix eggs and cheese"),
                        new PreparationStep(3, "Combine everything")));
    }

    @Test
    void shouldPersistAndRetrieveRecipeWithChildren() {
        Recipe created = recipeService.create(newRecipe());

        assertThat(created.id()).isNotNull();

        Recipe found = recipeService.getById(created.id());
        assertThat(found.title()).isEqualTo("Spaghetti Carbonara");
        assertThat(found.ingredients()).hasSize(2)
                .extracting(Ingredient::name)
                .containsExactly("Spaghetti", "Eggs");
        assertThat(found.steps()).hasSize(3)
                .extracting(PreparationStep::position)
                .containsExactly(1, 2, 3);
    }

    @Test
    void shouldUpdateRecipeAndReplaceChildren() {
        Recipe created = recipeService.create(newRecipe());

        Recipe changed = new Recipe(
                null,
                "Vegan Carbonara",
                "Plant-based twist",
                2,
                20,
                Difficulty.HARD,
                List.of(new Ingredient("Tofu", new BigDecimal("200"), "g")),
                List.of(new PreparationStep(1, "Blend the tofu")));

        Recipe updated = recipeService.update(created.id(), changed);

        assertThat(updated.title()).isEqualTo("Vegan Carbonara");
        assertThat(updated.difficulty()).isEqualTo(Difficulty.HARD);
        assertThat(updated.ingredients()).extracting(Ingredient::name).containsExactly("Tofu");
        assertThat(updated.steps()).hasSize(1);
    }

    @Test
    void shouldDeleteRecipe() {
        Recipe created = recipeService.create(newRecipe());
        long id = created.id();

        recipeService.delete(id);

        assertThatThrownBy(() -> recipeService.getById(id))
                .isInstanceOf(RecipeNotFoundException.class);
    }

    @Test
    void shouldReturnAllRecipes() {
        int before = recipeService.getAll().size();

        recipeService.create(newRecipe());
        recipeService.create(newRecipe());

        assertThat(recipeService.getAll()).hasSize(before + 2);
    }
}
