package io.github.keymaster65.helloai.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.github.keymaster65.helloai.application.service.RecipeNotFoundException;
import io.github.keymaster65.helloai.bootstrap.RecipeApplication;
import io.github.keymaster65.helloai.domain.model.Difficulty;
import io.github.keymaster65.helloai.domain.model.Recipe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link RecipeController} using the real mapper and a mocked service.
 */
@WebMvcTest(RecipeController.class)
@ContextConfiguration(classes = RecipeApplication.class)
@Import(RecipeRestMapper.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @Test
    void shouldReturn201AndLocation_whenCreatingValidRecipe() throws Exception {
        Recipe saved = new Recipe(1L, "Pancakes", "Fluffy", 2, 15, Difficulty.EASY, List.of(), List.of());
        when(recipeService.create(any())).thenReturn(saved);

        String body = """
                {"title":"Pancakes","description":"Fluffy","servings":2,
                 "prepTimeMinutes":15,"difficulty":"EASY","ingredients":[],"steps":[]}
                """;

        mockMvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Pancakes"));
    }

    @Test
    void shouldReturn400_whenTitleIsBlank() throws Exception {
        String body = """
                {"title":"  ","difficulty":"EASY"}
                """;

        mockMvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldReturn400_whenDifficultyMissing() throws Exception {
        String body = """
                {"title":"Pancakes"}
                """;

        mockMvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400_whenNestedIngredientIsInvalid() throws Exception {
        String body = """
                {"title":"Pancakes","difficulty":"EASY",
                 "ingredients":[{"name":"Mehl"},{"name":"  "}],"steps":[]}
                """;

        mockMvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("ingredients[1].name"));
    }

    @Test
    void shouldReturn400_whenNestedStepIsInvalid() throws Exception {
        String body = """
                {"title":"Pancakes","difficulty":"EASY",
                 "ingredients":[],"steps":[{"instruction":""}]}
                """;

        mockMvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("steps[0].instruction"));
    }

    @Test
    void shouldReturn404_whenRecipeNotFound() throws Exception {
        when(recipeService.getById(99L)).thenThrow(new RecipeNotFoundException(99L));

        mockMvc.perform(get("/api/recipes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn204_whenDeletingRecipe() throws Exception {
        mockMvc.perform(delete("/api/recipes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404_whenDeletingMissingRecipe() throws Exception {
        doThrow(new RecipeNotFoundException(99L)).when(recipeService).delete(anyLong());

        mockMvc.perform(delete("/api/recipes/99"))
                .andExpect(status().isNotFound());
    }
}
