package io.github.keymaster65.helloai.systemtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import tools.jackson.databind.JsonNode;

/**
 * System tests for the MCP endpoint (see ADR 0049): the running application answers JSON-RPC 2.0
 * under {@code /recipes/api/mcp}, without a session and with four read-only tools.
 *
 * <p>Tested over HTTP exactly the way a client speaks to it – including the two rules of the
 * transport that are easy to get wrong: the {@code Accept} header must name both media types, and
 * a request carrying an {@code Origin} is refused.
 *
 * <p>Like {@link DocumentationSystemTest} these tests expect a deployable built <em>with</em> the
 * documentation; with {@code -PskipDocs} the resource catalogue is empty on purpose.
 */
@EnabledIf("applicationAvailable")
class McpSystemTest {

    private static final String MCP = "/api/mcp";

    /** Both media types are mandatory for streamable HTTP; the transport answers 400 without. */
    private static final Map<String, String> MCP_HEADERS =
            Map.of("Accept", "application/json, text/event-stream");

    /** The version this test speaks; the server answers with the version it agreed on. */
    private static final String PROTOCOL_VERSION = "2025-06-18";

    static boolean applicationAvailable() {
        return RunningApplication.available();
    }

    private static JsonNode call(String method, String params) {
        String body = """
                {"jsonrpc":"2.0","id":1,"method":"%s","params":%s}""".formatted(method, params);
        HttpResponse<String> response = HttpProbe.post(MCP, body, MCP_HEADERS);

        assertThat(response.statusCode()).as("HTTP status of %s", method).isEqualTo(200);
        JsonNode message = HttpProbe.parse(response.body());
        assertThat(message.path("error").isMissingNode()).as("JSON-RPC error of %s: %s", method, message).isTrue();
        return message.path("result");
    }

    /** Reads the single text content of a tool result – the tools answer with one block. */
    private static String textOf(JsonNode toolResult) {
        assertThat(toolResult.path("isError").asBoolean(false)).as("tool reported an error: %s", toolResult).isFalse();
        return toolResult.path("content").path(0).path("text").asString();
    }

    @Test
    void shouldIntroduceItselfOnInitialize() {
        JsonNode result = call("initialize", """
                {"protocolVersion":"%s","capabilities":{},\
                "clientInfo":{"name":"recipes-systemtest","version":"1"}}""".formatted(PROTOCOL_VERSION));

        assertThat(result.path("protocolVersion").asString()).isNotBlank();
        assertThat(result.path("serverInfo").path("name").asString()).isEqualTo("recipes");
        assertThat(result.path("capabilities").path("tools").isMissingNode()).as("tools capability").isFalse();
        assertThat(result.path("capabilities").path("resources").isMissingNode()).as("resources capability").isFalse();
        // The instructions tell a client where to start; an empty string would be a silent loss.
        assertThat(result.path("instructions").asString()).contains("list_recipes");
    }

    @Test
    void shouldOfferTheFourReadOnlyTools() {
        JsonNode tools = call("tools/list", "{}").path("tools");

        assertThat(tools).hasSize(4);
        assertThat(tools.valueStream().map(tool -> tool.path("name").asString()).toList())
                .containsExactlyInAnyOrder("list_recipes", "get_recipe", "list_documentation", "read_documentation");
        assertThat(tools).allSatisfy(tool ->
                assertThat(tool.path("annotations").path("readOnlyHint").asBoolean(false))
                        .as("read-only hint of %s", tool.path("name").asString())
                        .isTrue());
    }

    @Test
    void shouldReturnTheSeededRecipesAsJson() {
        String recipes = textOf(call("tools/call", """
                {"name":"list_recipes","arguments":{}}"""));

        // The changelog seeds recipes (0002-seed-fasting-recipes.xml), so the list is not empty.
        JsonNode parsed = HttpProbe.parse(recipes);
        assertThat(parsed.isArray()).as("list_recipes returns an array: %s", recipes).isTrue();
        assertThat(parsed).isNotEmpty();
        assertThat(parsed.path(0).path("title").asString()).isNotBlank();
    }

    @Test
    void shouldReturnOneRecipeWithIngredientsAndSteps() {
        JsonNode recipes = HttpProbe.parse(textOf(call("tools/call", """
                {"name":"list_recipes","arguments":{}}""")));
        long id = recipes.path(0).path("id").asLong();

        JsonNode recipe = HttpProbe.parse(textOf(call("tools/call", """
                {"name":"get_recipe","arguments":{"id":%d}}""".formatted(id))));

        assertThat(recipe.path("id").asLong()).isEqualTo(id);
        assertThat(recipe.path("ingredients").isArray()).isTrue();
        assertThat(recipe.path("steps").isArray()).isTrue();
    }

    @Test
    void shouldReportAnUnknownRecipeAsToolError() {
        JsonNode result = call("tools/call", """
                {"name":"get_recipe","arguments":{"id":999999}}""");

        // A missing recipe is an error of the call, not of the protocol – and it stays sparse.
        assertThat(result.path("isError").asBoolean(false)).isTrue();
        String message = result.path("content").path(0).path("text").asString();
        assertThat(message).contains("999999");
        assertThat(message).doesNotContain("Exception", "io.github");
    }

    @Test
    void shouldOfferTheDeliveredDocumentationAsResources() {
        JsonNode resources = call("resources/list", "{}").path("resources");

        assertThat(resources).isNotEmpty();
        assertThat(resources.valueStream().map(resource -> resource.path("uri").asString()))
                .allSatisfy(uri -> assertThat(uri).startsWith("recipes://docs/"))
                .contains("recipes://docs/system");
    }

    @Test
    void shouldReadTheChaptersAsResource() {
        JsonNode contents = call("resources/read", """
                {"uri":"recipes://docs/system"}""").path("contents").path(0);

        assertThat(contents.path("mimeType").asString()).isEqualTo("text/html");
        assertThat(contents.path("text").asString()).contains("Systemdokumentation: recipes");
    }

    @Test
    void shouldListTheSamePagesAsTool() {
        JsonNode pages = HttpProbe.parse(textOf(call("tools/call", """
                {"name":"list_documentation","arguments":{}}""")));

        assertThat(pages).isNotEmpty();
        assertThat(pages.valueStream().map(page -> page.path("id").asString())).contains("system");
    }

    @Test
    void shouldRefuseARequestCarryingAnOrigin() {
        // Protection against DNS rebinding: a browser announces its origin, a tool does not.
        Map<String, String> browserLike = Map.of(
                "Accept", "application/json, text/event-stream",
                "Origin", "http://evil.example.org");

        HttpResponse<String> response = HttpProbe.post(MCP, """
                {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}""", browserLike);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void shouldRefuseARequestWithoutTheRequiredAcceptHeader() {
        HttpResponse<String> response = HttpProbe.post(MCP, """
                {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}""",
                Map.of("Accept", "application/json"));

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void shouldNotAnswerGetOnTheEndpoint() {
        // Without a session there is nothing to stream, so the transport allows POST only.
        assertThat(HttpProbe.get(MCP).statusCode()).isEqualTo(405);
    }
}
