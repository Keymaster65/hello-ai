package io.github.keymaster65.helloai.adapter.in.mcp;

import static io.github.keymaster65.helloai.adapter.in.mcp.McpToolResults.NO_ARGUMENTS;
import static io.github.keymaster65.helloai.adapter.in.mcp.McpToolResults.failure;
import static io.github.keymaster65.helloai.adapter.in.mcp.McpToolResults.json;
import static io.github.keymaster65.helloai.adapter.in.mcp.McpToolResults.longArgument;

import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.github.keymaster65.helloai.application.service.RecipeNotFoundException;
import io.github.keymaster65.helloai.domain.model.Ingredient;
import io.github.keymaster65.helloai.domain.model.PreparationStep;
import io.github.keymaster65.helloai.domain.model.Recipe;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The recipes as MCP tools (ADR 0049): {@code list_recipes} and {@code get_recipe}.
 *
 * <p>Inbound adapter like the REST controller, and as thin as it: it reaches the use cases through
 * the {@link RecipeService} port and turns their result into JSON. It knows nothing of the REST
 * adapter next to it &ndash; two adapters do not share their DTOs (ADR 0019), so the shape of the
 * JSON here is chosen for a language model, not for a browser.
 *
 * <p>Recipes are tools rather than resources on purpose: the resource catalogue of a stateless
 * server is fixed at start-up, and a recipe created afterwards would be missing from it.
 */
@Component
class RecipeMcpTools {

    private static final String LIST_TOOL = "list_recipes";
    private static final String GET_TOOL = "get_recipe";
    private static final String ID_ARGUMENT = "id";

    /** A whole number, at least 1 &ndash; the SDK rejects anything else before the handler runs. */
    private static final Map<String, Object> ID_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    ID_ARGUMENT, Map.of(
                            "type", "integer",
                            "minimum", 1,
                            "description", "Identifier of the recipe, as returned by list_recipes")),
            "required", List.of(ID_ARGUMENT),
            "additionalProperties", false);

    private final RecipeService recipeService;
    private final McpJsonMapper jsonMapper;

    RecipeMcpTools(RecipeService recipeService, McpJsonMapper jsonMapper) {
        this.recipeService = recipeService;
        this.jsonMapper = jsonMapper;
    }

    /**
     * The tool specifications this adapter contributes to the server.
     *
     * @return the specifications, in the order a client should discover them
     */
    List<SyncToolSpecification> specifications() {
        return List.of(listRecipes(), getRecipe());
    }

    private SyncToolSpecification listRecipes() {
        McpSchema.Tool tool = McpSchema.Tool.builder(LIST_TOOL, NO_ARGUMENTS)
                .title("List recipes")
                .description("""
                        Lists every recipe with its identifier, title, description, servings, \
                        preparation time and difficulty. Ingredients and preparation steps are \
                        left out; get_recipe returns them for a single recipe.""")
                .annotations(readOnly())
                .build();

        return SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((context, request) ->
                        json(jsonMapper, recipeService.getAll().stream().map(RecipeMcpTools::summary).toList()))
                .build();
    }

    private SyncToolSpecification getRecipe() {
        McpSchema.Tool tool = McpSchema.Tool.builder(GET_TOOL, ID_SCHEMA)
                .title("Get a recipe")
                .description("""
                        Returns one recipe including its ingredients and its ordered preparation \
                        steps. Takes the identifier from list_recipes.""")
                .annotations(readOnly())
                .build();

        return SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((context, request) -> longArgument(request, ID_ARGUMENT)
                        .map(this::recipe)
                        .orElseGet(() -> failure("The argument id is required and must be a whole number")))
                .build();
    }

    private McpSchema.CallToolResult recipe(long id) {
        try {
            return json(jsonMapper, detail(recipeService.getById(id)));
        } catch (RecipeNotFoundException e) {
            return failure("No recipe exists with the identifier " + e.id());
        }
    }

    /** Reading, and the set of recipes is closed &ndash; hints a client may show to its user. */
    private static McpSchema.ToolAnnotations readOnly() {
        return McpSchema.ToolAnnotations.builder()
                .readOnlyHint(true)
                .openWorldHint(false)
                .build();
    }

    private static Map<String, Object> summary(Recipe recipe) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", recipe.id());
        json.put("title", recipe.title());
        json.put("description", recipe.description());
        json.put("servings", recipe.servings());
        json.put("prepTimeMinutes", recipe.prepTimeMinutes());
        json.put("difficulty", recipe.difficulty().name());
        return json;
    }

    private static Map<String, Object> detail(Recipe recipe) {
        Map<String, Object> json = summary(recipe);
        json.put("ingredients", recipe.ingredients().stream().map(RecipeMcpTools::ingredient).toList());
        json.put("steps", recipe.steps().stream().map(RecipeMcpTools::step).toList());
        return json;
    }

    private static Map<String, Object> ingredient(Ingredient ingredient) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", ingredient.name());
        json.put("quantity", ingredient.quantity());
        json.put("unit", ingredient.unit());
        return json;
    }

    private static Map<String, Object> step(PreparationStep step) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("position", step.position());
        json.put("instruction", step.instruction());
        return json;
    }
}
