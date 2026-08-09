package io.github.keymaster65.helloai.systemtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import tools.jackson.databind.JsonNode;

/**
 * System tests for the Swagger/OpenAPI surface of a running application (see ADR 0005 and 0006).
 *
 * <p>Everything here goes through HTTP against {@link RunningApplication#baseUrl()}, so the tests
 * are equally valid against a locally booted instance and against a deployed environment.
 */
@EnabledIf("applicationAvailable")
class SwaggerSystemTest {

    private static final String API_DOCS = "/v3/api-docs";

    static boolean applicationAvailable() {
        return RunningApplication.available();
    }

    @Test
    void shouldServeOpenApiDocumentWithApplicationMetadata() {
        HttpResponse<String> response = HttpProbe.get(API_DOCS);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                contentType -> assertThat(contentType).contains("json"));

        JsonNode document = HttpProbe.getJson(API_DOCS);
        assertThat(document.path("openapi").asString()).startsWith("3.1");
        assertThat(document.path("info").path("title").asString()).isEqualTo("Recipe API");
        assertThat(document.path("info").path("version").asString()).isNotBlank();
        assertThat(document.path("info").path("license").path("name").asString()).isEqualTo("Apache-2.0");
    }

    @Test
    void shouldDocumentEveryRecipeOperationWithItsStatusCodes() {
        JsonNode paths = HttpProbe.getJson(API_DOCS).path("paths");

        assertThat(paths.path("/api/recipes").propertyNames()).containsExactlyInAnyOrder("get", "post");
        assertThat(paths.path("/api/recipes/{id}").propertyNames())
                .containsExactlyInAnyOrder("get", "put", "delete");

        assertThat(responseCodes(paths, "/api/recipes", "post")).containsExactlyInAnyOrder("201", "400");
        assertThat(responseCodes(paths, "/api/recipes", "get")).containsExactly("200");
        assertThat(responseCodes(paths, "/api/recipes/{id}", "get")).containsExactlyInAnyOrder("200", "404");
        assertThat(responseCodes(paths, "/api/recipes/{id}", "put"))
                .containsExactlyInAnyOrder("200", "400", "404");
        assertThat(responseCodes(paths, "/api/recipes/{id}", "delete"))
                .containsExactlyInAnyOrder("204", "404");
    }

    @Test
    void shouldExposeDtoSchemasIncludingValidationConstraints() {
        JsonNode schemas = HttpProbe.getJson(API_DOCS).path("components").path("schemas");

        assertThat(schemas.propertyNames()).contains(
                "RecipeRequest", "RecipeResponse", "Ingredient", "PreparationStep",
                "PreparationStepResponse", "ErrorResponse", "FieldError");

        // Derived from @NotBlank / @NotNull on RecipeRequest – proof that validation and contract agree.
        assertThat(schemas.path("RecipeRequest").path("required").valueStream()
                .map(JsonNode::asString).toList())
                .containsExactlyInAnyOrder("title", "difficulty");

        // No domain or jOOQ type may leak into the published contract.
        assertThat(schemas.propertyNames()).doesNotContain("Recipe", "RecipeRecord");
    }

    @Test
    void shouldMarkGuaranteedResponseFieldsAsRequired() {
        JsonNode schemas = HttpProbe.getJson(API_DOCS).path("components").path("schemas");

        // Guaranteed by the domain model, so generated clients may type them as non-optional.
        assertThat(requiredOf(schemas, "RecipeResponse")).containsExactlyInAnyOrder(
                "id", "title", "difficulty", "ingredients", "steps");
        assertThat(requiredOf(schemas, "PreparationStepResponse"))
                .containsExactlyInAnyOrder("position", "instruction");
        assertThat(requiredOf(schemas, "ErrorResponse"))
                .containsExactlyInAnyOrder("status", "error", "message", "fieldErrors");
        assertThat(requiredOf(schemas, "FieldError")).containsExactlyInAnyOrder("field", "message");

        // Nullable in the domain – must stay optional so the contract does not overpromise.
        assertThat(requiredOf(schemas, "RecipeResponse"))
                .doesNotContain("description", "servings", "prepTimeMinutes");
    }

    @Test
    void shouldServeOpenApiDocumentAsYaml() {
        HttpResponse<String> response = HttpProbe.get(API_DOCS + ".yaml");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("openapi:", "/api/recipes");
    }

    @Test
    void shouldRedirectSwaggerUiEntryPointToTheUiPage() {
        HttpResponse<String> redirect = HttpProbe.get("/swagger-ui.html");

        assertThat(redirect.statusCode()).isEqualTo(302);
        assertThat(redirect.headers().firstValue("Location")).hasValueSatisfying(
                location -> assertThat(location).endsWith("/swagger-ui/index.html"));

        HttpResponse<String> page = HttpProbe.get("/swagger-ui/index.html");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("<div id=\"swagger-ui\">");
    }

    @Test
    void shouldServeSwaggerUiAssets() {
        List.of("/swagger-ui/swagger-ui.css",
                        "/swagger-ui/swagger-ui-bundle.js",
                        "/swagger-ui/swagger-ui-standalone-preset.js")
                .forEach(asset -> {
                    HttpResponse<String> response = HttpProbe.get(asset);
                    assertThat(response.statusCode()).as(asset).isEqualTo(200);
                    assertThat(response.body()).as(asset).isNotEmpty();
                });
    }

    @Test
    void shouldPointSwaggerUiAtThisApplicationsContract() {
        // The stock WebJar initializer points at petstore.swagger.io; springdoc has to override that
        // via configUrl, otherwise the UI would render a foreign API. Both values must carry the
        // context path (ADR 0016) – otherwise the UI would look for the contract at the wrong place.
        String contextPath = RunningApplication.CONTEXT_PATH;
        assertThat(HttpProbe.get("/swagger-ui/swagger-initializer.js").body())
                .contains("\"configUrl\" : \"" + contextPath + "/v3/api-docs/swagger-config\"");

        JsonNode config = HttpProbe.getJson("/v3/api-docs/swagger-config");
        assertThat(config.path("url").asString()).isEqualTo(contextPath + API_DOCS);
    }

    @Test
    void shouldBehaveAsDocumentedForListAndUnknownRecipe() {
        JsonNode paths = HttpProbe.getJson(API_DOCS).path("paths");

        // Documented: GET /api/recipes -> 200
        assertThat(responseCodes(paths, "/api/recipes", "get")).contains("200");
        assertThat(HttpProbe.get("/api/recipes").statusCode()).isEqualTo(200);

        // Documented: GET /api/recipes/{id} -> 404 for an unknown id, shaped like ErrorResponse
        assertThat(responseCodes(paths, "/api/recipes/{id}", "get")).contains("404");
        HttpResponse<String> notFound = HttpProbe.get("/api/recipes/999999");
        assertThat(notFound.statusCode()).isEqualTo(404);

        JsonNode error = HttpProbe.getJson("/api/recipes/999999");
        assertThat(error.propertyNames()).containsExactlyInAnyOrderElementsOf(
                HttpProbe.getJson(API_DOCS).path("components").path("schemas")
                        .path("ErrorResponse").path("properties").propertyNames());
    }

    @Test
    void shouldRejectInvalidRecipeAsDocumented() {
        HttpResponse<String> response = HttpProbe.post("/api/recipes", "{\"title\":\"\"}");

        assertThat(response.statusCode()).isEqualTo(400);

        JsonNode error = HttpProbe.getJson(API_DOCS).path("paths").path("/api/recipes")
                .path("post").path("responses");
        assertThat(error.propertyNames()).contains("400");
    }

    private static List<String> responseCodes(JsonNode paths, String path, String method) {
        return List.copyOf(paths.path(path).path(method).path("responses").propertyNames());
    }

    private static List<String> requiredOf(JsonNode schemas, String schema) {
        return schemas.path(schema).path("required").valueStream().map(JsonNode::asString).toList();
    }
}
