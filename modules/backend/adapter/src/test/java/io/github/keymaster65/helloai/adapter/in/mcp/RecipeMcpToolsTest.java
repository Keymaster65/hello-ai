package io.github.keymaster65.helloai.adapter.in.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.github.keymaster65.helloai.application.service.RecipeNotFoundException;
import io.github.keymaster65.helloai.domain.model.Difficulty;
import io.github.keymaster65.helloai.domain.model.Ingredient;
import io.github.keymaster65.helloai.domain.model.PreparationStep;
import io.github.keymaster65.helloai.domain.model.Recipe;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The recipe tools of the MCP adapter: what a client sees in {@code tools/list} and what a
 * {@code tools/call} answers. The use cases behind them are mocked – this is the adapter under
 * test, not the application layer.
 */
@ExtendWith(MockitoExtension.class)
class RecipeMcpToolsTest {

    private static final McpJsonMapper JSON = McpJsonDefaults.getMapper();

    @Mock
    private RecipeService recipeService;

    private RecipeMcpTools tools() {
        return new RecipeMcpTools(recipeService, JSON);
    }

    private static Recipe pancakes(Long id) {
        return new Recipe(
                id,
                "Pancakes",
                "Fluffy",
                2,
                15,
                Difficulty.EASY,
                List.of(new Ingredient("Flour", BigDecimal.valueOf(300), "g")),
                List.of(new PreparationStep(1, "Mix everything")));
    }

    private McpSchema.CallToolResult call(String name, Map<String, Object> arguments) {
        SyncToolSpecification specification = tools().specifications().stream()
                .filter(candidate -> candidate.tool().name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No tool named " + name));
        return specification.callHandler()
                .apply(McpTransportContext.EMPTY, new McpSchema.CallToolRequest(name, arguments, null));
    }

    private static String text(McpSchema.CallToolResult result) {
        assertThat(result.content()).hasSize(1);
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }

    @Test
    void shouldOfferTwoReadOnlyTools() {
        // Act
        List<SyncToolSpecification> specifications = tools().specifications();

        // Assert – the names are the contract; a rename is visible to every client.
        assertThat(specifications).extracting(specification -> specification.tool().name())
                .containsExactly("list_recipes", "get_recipe");
        assertThat(specifications).allSatisfy(specification -> {
            assertThat(specification.tool().annotations().readOnlyHint()).isTrue();
            assertThat(specification.tool().description()).isNotBlank();
        });
    }

    @Test
    void shouldRequireTheIdentifier_whenGettingARecipe() {
        // Assert – the schema is what the SDK validates a call against.
        McpSchema.Tool getRecipe = tools().specifications().get(1).tool();

        assertThat(getRecipe.inputSchema()).containsEntry("required", List.of("id"));
        assertThat(getRecipe.inputSchema()).containsEntry("additionalProperties", false);
    }

    @Test
    void shouldListRecipesWithoutIngredientsAndSteps() {
        // Arrange
        when(recipeService.getAll()).thenReturn(List.of(pancakes(1L)));

        // Act
        String json = text(call("list_recipes", Map.of()));

        // Assert
        assertThat(json).contains("\"id\":1", "\"title\":\"Pancakes\"", "\"difficulty\":\"EASY\"");
        assertThat(json).doesNotContain("ingredients", "steps");
    }

    @Test
    void shouldListEmptyArray_whenNoRecipesExist() {
        // Arrange
        when(recipeService.getAll()).thenReturn(List.of());

        // Act
        McpSchema.CallToolResult result = call("list_recipes", Map.of());

        // Assert – an empty result is not an error.
        assertThat(text(result)).isEqualTo("[]");
        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
    }

    @Test
    void shouldReturnIngredientsAndSteps_whenGettingARecipe() {
        // Arrange
        when(recipeService.getById(1L)).thenReturn(pancakes(1L));

        // Act
        String json = text(call("get_recipe", Map.of("id", 1)));

        // Assert
        assertThat(json).contains("\"name\":\"Flour\"", "\"unit\":\"g\"", "\"position\":1",
                "\"instruction\":\"Mix everything\"");
    }

    @Test
    void shouldReportAnError_whenTheRecipeDoesNotExist() {
        // Arrange
        when(recipeService.getById(99L)).thenThrow(new RecipeNotFoundException(99L));

        // Act
        McpSchema.CallToolResult result = call("get_recipe", Map.of("id", 99));

        // Assert – the identifier, nothing from inside the application.
        assertThat(result.isError()).isTrue();
        assertThat(text(result)).isEqualTo("No recipe exists with the identifier 99");
        assertThat(text(result)).doesNotContain("Exception", "io.github");
    }

    @Test
    void shouldReportAnError_whenTheIdentifierIsMissing() {
        // Act – the SDK validates the schema first, so this is the second net.
        McpSchema.CallToolResult result = call("get_recipe", Map.of());

        // Assert
        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("id");
    }

    @Test
    void shouldReportAnError_whenTheIdentifierIsNotANumber() {
        // Act
        McpSchema.CallToolResult result = call("get_recipe", Map.of("id", "eins"));

        // Assert
        assertThat(result.isError()).isTrue();
    }
}
