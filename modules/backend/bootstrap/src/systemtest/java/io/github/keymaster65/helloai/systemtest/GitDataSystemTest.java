package io.github.keymaster65.helloai.systemtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import tools.jackson.databind.JsonNode;

/**
 * The second REST front over HTTP: the git-backed store, addressed like any other API
 * (ADR 0053, ADR 0054).
 *
 * <p>What only shows from outside is the coexistence: two fronts, two stores, one contract. A
 * recipe created here must <em>not</em> appear under {@code /api/recipes} &ndash; if it did, both
 * addresses would be talking to the same store and the whole exercise would be decoration.
 *
 * <p>The self-hosted instance switches the store on into a throw-away directory
 * ({@link RunningApplication}). Against an <em>external</em> instance the address may legitimately
 * be absent, because the store is off by default; the tests then skip instead of failing.
 */
@EnabledIf("applicationAvailable")
class GitDataSystemTest {

    private static final String GITDATA = "/api/gitdata/recipes";
    private static final String RELATIONAL = "/api/recipes";

    private static final String TEA = """
            {
              "title": "Systemtest-Fastentee",
              "servings": 1,
              "difficulty": "EASY",
              "ingredients": [{"name": "Fenchelsamen", "quantity": 1, "unit": "TL"}],
              "steps": [{"instruction": "Aufgießen"}]
            }
            """;

    static boolean applicationAvailable() {
        return RunningApplication.available();
    }

    /** Skips against a deployment that does not run the store &ndash; the default is off. */
    private static void requireStore() {
        Assumptions.assumeTrue(
                HttpProbe.get(GITDATA).statusCode() != 404,
                "recipes.gitdata.enabled is off on this instance");
    }

    @Test
    void shouldCreateReadAndDeleteThroughTheGitBackedFront() {
        requireStore();

        HttpResponse<String> created = HttpProbe.post(GITDATA, TEA);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.headers().firstValue("Location").orElseThrow())
                .as("der Location-Header zeigt auf diese Front, nicht auf die relationale")
                .contains(GITDATA + "/");

        JsonNode recipe = HttpProbe.parse(created.body());
        long id = recipe.path("id").asLong();
        assertThat(recipe.path("title").asString()).isEqualTo("Systemtest-Fastentee");
        assertThat(recipe.path("ingredients").path(0).path("name").asString()).isEqualTo("Fenchelsamen");

        JsonNode readBack = HttpProbe.getJson(GITDATA + "/" + id);
        assertThat(readBack.path("title").asString()).isEqualTo("Systemtest-Fastentee");
        assertThat(readBack.path("steps").path(0).path("position").asInt()).isEqualTo(1);

        assertThat(HttpProbe.delete(GITDATA + "/" + id).statusCode()).isEqualTo(204);
        assertThat(HttpProbe.get(GITDATA + "/" + id).statusCode()).isEqualTo(404);
    }

    @Test
    void shouldKeepTheTwoStoresApart() {
        requireStore();

        JsonNode created = HttpProbe.parse(HttpProbe.post(GITDATA, TEA).body());
        long id = created.path("id").asLong();
        try {
            assertThat(HttpProbe.getJson(RELATIONAL).valueStream()
                    .map(entry -> entry.path("title").asString())
                    .toList())
                    .as("was im git-Store liegt, taucht in der Datenbank nicht auf")
                    .doesNotContain("Systemtest-Fastentee");
        } finally {
            HttpProbe.delete(GITDATA + "/" + id);
        }
    }

    @Test
    void shouldAnswerAnUnknownRecipeAsProblemDetail() {
        requireStore();

        HttpResponse<String> response = HttpProbe.get(GITDATA + "/999999");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/problem+json");
        // The message names the identifier that was asked for and nothing about the store behind it.
        assertThat(HttpProbe.parse(response.body()).path("detail").asString())
                .contains("999999")
                .doesNotContain("git", "database/entities");
    }

    @Test
    void shouldDescribeTheSecondFrontInTheContract() {
        requireStore();

        JsonNode paths = HttpProbe.getJson("/v3/api-docs").path("paths");

        assertThat(paths.has(GITDATA)).as("die zweite Front steht im Contract").isTrue();
        assertThat(paths.has(RELATIONAL)).as("die erste ebenfalls").isTrue();
    }
}
