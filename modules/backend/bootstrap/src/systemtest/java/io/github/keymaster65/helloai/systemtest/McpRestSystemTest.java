package io.github.keymaster65.helloai.systemtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import tools.jackson.databind.JsonNode;

/**
 * System tests for the REST facade of the MCP server (see ADR 0050): the same tools and resources
 * as {@link McpSystemTest}, but as ordinary HTTP operations that appear in the OpenAPI contract and
 * therefore in Swagger UI.
 *
 * <p>Two things are only visible from outside and are checked here: that the facade and the
 * JSON-RPC endpoint coexist under the same prefix &ndash; the transport servlet is mapped to the
 * exact path {@code /api/mcp}, so everything below it reaches Spring MVC &ndash; and that the
 * contract really carries the operations.
 *
 * <p>Like {@link DocumentationSystemTest} these tests expect a deployable built <em>with</em> the
 * documentation; with {@code -PskipDocs} the resource list is empty on purpose.
 */
@EnabledIf("applicationAvailable")
class McpRestSystemTest {

    private static final String MCP = "/api/mcp";
    private static final String TOOLS = MCP + "/tools";
    private static final String RESOURCES = MCP + "/resources";

    static boolean applicationAvailable() {
        return RunningApplication.available();
    }

    private static JsonNode callTool(String name, String arguments) {
        HttpResponse<String> response = HttpProbe.post(TOOLS + "/" + name, arguments);

        assertThat(response.statusCode()).as("HTTP status of %s", name).isEqualTo(200);
        return HttpProbe.parse(response.body());
    }

    /** Reads the single text block of a tool result – the tools of this server answer with one. */
    private static String textOf(JsonNode result) {
        assertThat(result.path("isError").asBoolean(false)).as("tool reported an error: %s", result).isFalse();
        return result.path("content").path(0).asString();
    }

    private static String contentOf(String uri) {
        return RESOURCES + "/content?uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8);
    }

    @Test
    void shouldIntroduceTheServer() {
        JsonNode info = HttpProbe.getJson(MCP + "/server");

        assertThat(info.path("name").asString()).isEqualTo("recipes");
        assertThat(info.path("version").asString()).isNotBlank();
        assertThat(info.path("instructions").asString()).contains("list_recipes");
    }

    @Test
    void shouldOfferTheSameFourToolsAsTheProtocolEndpoint() {
        JsonNode tools = HttpProbe.getJson(TOOLS);

        assertThat(tools.valueStream().map(tool -> tool.path("name").asString()).toList())
                .containsExactlyInAnyOrder("list_recipes", "get_recipe", "list_documentation", "read_documentation");
        // The input schema is what tells a caller which arguments to send; it must survive the facade.
        assertThat(tools).allSatisfy(tool ->
                assertThat(tool.path("inputSchema").path("type").asString())
                        .as("input schema of %s", tool.path("name").asString())
                        .isEqualTo("object"));
    }

    @Test
    void shouldReturnOneRecipeWithIngredientsAndSteps() {
        JsonNode recipes = HttpProbe.parse(textOf(callTool("list_recipes", "{}")));
        assertThat(recipes).isNotEmpty();
        long id = recipes.path(0).path("id").asLong();

        JsonNode recipe = HttpProbe.parse(textOf(callTool("get_recipe", "{\"id\":%d}".formatted(id))));

        assertThat(recipe.path("id").asLong()).isEqualTo(id);
        assertThat(recipe.path("ingredients").isArray()).isTrue();
        assertThat(recipe.path("steps").isArray()).isTrue();
    }

    @Test
    void shouldReportAnUnknownRecipeAsToolErrorWithStatusOk() {
        // The tool ran and reported a failure – that is a result, not a transport error.
        JsonNode result = callTool("get_recipe", "{\"id\":999999}");

        assertThat(result.path("isError").asBoolean(false)).isTrue();
        String message = result.path("content").path(0).asString();
        assertThat(message).contains("999999");
        assertThat(message).doesNotContain("Exception", "io.github");
    }

    @Test
    void shouldReportArgumentsAgainstTheSchemaAsToolError() {
        JsonNode result = callTool("get_recipe", "{\"id\":\"eins\"}");

        assertThat(result.path("isError").asBoolean(false)).isTrue();
    }

    @Test
    void shouldAnswerNotFoundForAnUnknownTool() {
        HttpResponse<String> response = HttpProbe.post(TOOLS + "/drop_recipes", "{}");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                contentType -> assertThat(contentType).startsWith("application/problem+json"));
    }

    @Test
    void shouldListAndReadTheDocumentationAsResources() {
        JsonNode resources = HttpProbe.getJson(RESOURCES);

        assertThat(resources).isNotEmpty();
        assertThat(resources.valueStream().map(resource -> resource.path("uri").asString()))
                .allSatisfy(uri -> assertThat(uri).startsWith("recipes://docs/"))
                .contains("recipes://docs/system");

        JsonNode contents = HttpProbe.getJson(contentOf("recipes://docs/system")).path(0);
        assertThat(contents.path("mimeType").asString()).isEqualTo("text/html");
        assertThat(contents.path("text").asString()).contains("Systemdokumentation: recipes");
    }

    @Test
    void shouldAnswerNotFoundForAnUnknownResource() {
        assertThat(HttpProbe.get(contentOf("recipes://docs/nirgendwo")).statusCode()).isEqualTo(404);
    }

    @Test
    void shouldLeaveTheProtocolEndpointUntouched() {
        // The transport servlet is mapped to the exact path, the facade lives below it. Both answer.
        HttpResponse<String> rpc = HttpProbe.post(MCP,
                """
                        {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}""",
                Map.of("Accept", "application/json, text/event-stream"));

        assertThat(rpc.statusCode()).isEqualTo(200);
        assertThat(HttpProbe.parse(rpc.body()).path("result").path("tools")).hasSize(4);
        assertThat(HttpProbe.getJson(TOOLS)).hasSize(4);
    }

    @Test
    void shouldAnswerARequestCarryingAnOrigin() {
        // Deliberate difference to the protocol endpoint, which refuses this with 403: a request
        // through Spring MVC does not pass the transport's origin check (ADR 0050, "Sicherheit").
        HttpResponse<String> response = HttpProbe.post(TOOLS + "/list_recipes", "{}",
                Map.of("Origin", "http://evil.example.org"));

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void shouldAppearInTheOpenApiContract() {
        JsonNode paths = HttpProbe.getJson("/v3/api-docs").path("paths");

        assertThat(paths.propertyNames()).contains(
                MCP + "/server", TOOLS, TOOLS + "/{name}", RESOURCES, RESOURCES + "/content");
        assertThat(paths.path(TOOLS + "/{name}").path("post").path("responses").propertyNames())
                .containsExactlyInAnyOrder("200", "404");
    }
}
