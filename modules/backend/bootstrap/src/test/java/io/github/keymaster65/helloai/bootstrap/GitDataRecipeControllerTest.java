package io.github.keymaster65.helloai.bootstrap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The second REST front, wired to the git-backed store (ADR 0054).
 *
 * <p>What this test is about is the <em>wiring</em>, not the store: that switching
 * {@code recipes.gitdata.enabled} on makes the address exist, that it is served by the second
 * use-case bean &ndash; no database is touched here, Liquibase is off &ndash; and that the contract
 * describes it. The behaviour of the store itself is tested where it lives, against a real
 * repository ({@code GitDataRecipeRepositoryTest}).
 *
 * <p>The repository is a temporary directory, handed in through {@link DynamicPropertySource}: a
 * fixed path would carry the rows of the last run into this one.
 */
@SpringBootTest(classes = RecipeApplication.class, properties = "spring.liquibase.enabled=false")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GitDataRecipeControllerTest {

    @TempDir
    static Path directory;

    private static final String PATH = "/api/gitdata/recipes";

    /** The six recipes the store is populated with at startup (ADR 0055). */
    private static final int SEEDED = 6;

    private static final String SOUP = """
            {
              "title": "Fastensuppe",
              "description": "Passiertes Gemüse",
              "servings": 2,
              "prepTimeMinutes": 30,
              "difficulty": "EASY",
              "ingredients": [{"name": "Karotten", "quantity": 150, "unit": "g"}],
              "steps": [{"instruction": "Gemüse putzen"}, {"instruction": "Passieren"}]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void gitDataStore(DynamicPropertyRegistry registry) {
        registry.add("recipes.gitdata.enabled", () -> true);
        registry.add("recipes.gitdata.repository", () -> directory.resolve("data.git").toString());
    }

    @Test
    @Order(1)
    void shouldStartWithTheSeededRecipes() throws Exception {
        // Not empty: the initial population runs before the first request (ADR 0055), out of the
        // same changeset Liquibase reads – here into a fresh, throw-away repository.
        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(SEEDED))
                .andExpect(jsonPath("$[0].title").value("Entlastungstag: Reis mit gedünstetem Gemüse"));
    }

    @Test
    @Order(2)
    void shouldCreateARecipeAndPointAtItsOwnAddress() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(SOUP))
                .andExpect(status().isCreated())
                // The Location header names *this* front, not the relational one.
                .andExpect(header().string(
                        "Location", org.hamcrest.Matchers.endsWith(PATH + "/" + (SEEDED + 1))))
                .andExpect(jsonPath("$.id").value(SEEDED + 1))
                .andExpect(jsonPath("$.title").value("Fastensuppe"))
                .andExpect(jsonPath("$.ingredients[0].name").value("Karotten"))
                .andExpect(jsonPath("$.steps[1].position").value(2));
    }

    @Test
    @Order(3)
    void shouldReadBackWhatTheStoreCommitted() throws Exception {
        mockMvc.perform(get(PATH + "/" + (SEEDED + 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Fastensuppe"))
                .andExpect(jsonPath("$.difficulty").value("EASY"));
    }

    @Test
    @Order(4)
    void shouldAnswerAnUnknownRecipeAsProblemDetail() throws Exception {
        mockMvc.perform(get(PATH + "/42"))
                .andExpect(status().isNotFound())
                .andExpect(content -> org.assertj.core.api.Assertions
                        .assertThat(content.getResponse().getContentType())
                        .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @Order(5)
    void shouldDeleteAndLeaveTheSeededRecipesBehind() throws Exception {
        mockMvc.perform(delete(PATH + "/" + (SEEDED + 1))).andExpect(status().isNoContent());
        mockMvc.perform(get(PATH)).andExpect(jsonPath("$.length()").value(SEEDED));
    }

    @Test
    @Order(6)
    void shouldKeepTheRelationalFrontMapped() throws Exception {
        // Two beans of each port exist now. The claim here is about wiring, not about data: the
        // primary front is still mapped, so nothing about it was taken over by the second one.
        // Whether the store behind it answers depends on a database this test does not provide,
        // so the status is deliberately not pinned down – only that the address is not gone.
        mockMvc.perform(get("/api/recipes"))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus())
                        .as("die primäre Front ist weiterhin abgebildet")
                        .isNotEqualTo(404));
    }

    @Test
    @Order(7)
    void shouldDescribeBothFrontsInTheContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/recipes'].get").exists())
                .andExpect(jsonPath("$.paths['" + PATH + "'].get").exists())
                .andExpect(jsonPath("$.paths['" + PATH + "'].post").exists())
                .andExpect(jsonPath("$.paths['" + PATH + "/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['" + PATH + "/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['" + PATH + "/{id}'].delete").exists());
    }
}
