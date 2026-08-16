package io.github.keymaster65.helloai.systemtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * System tests for the system documentation shipped inside the deployable (see
 * docs/prompt/systemdokumentation.adoc): the running application serves the rendered HTML
 * together with everything it references relatively.
 *
 * <p>These tests fail – deliberately – when the jar was built with {@code -PskipDocs}: the flag
 * buys a fast inner loop, not a documented deployable.
 */
@EnabledIf("applicationAvailable")
class DocumentationSystemTest {

    /**
     * Every resource the document embeds relatively – today the diagrams rendered from their
     * PlantUML source. Absolute references (a scheme, a leading slash, inline data) are none of
     * this test's business: they do not come out of the build.
     */
    private static final Pattern RELATIVE_RESOURCE =
            Pattern.compile("src=\"(?!https?:|//|/|data:)([^\"]+)\"");

    static boolean applicationAvailable() {
        return RunningApplication.available();
    }

    @Test
    void shouldServeTheSystemDocumentationUnderDocs() {
        HttpResponse<String> response = HttpProbe.get("/docs/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                contentType -> assertThat(contentType).contains("text/html"));
        // The title of the master document – if the appendix or a chapter went missing, the
        // Asciidoctor build would already have failed, so one anchor is enough here.
        assertThat(response.body()).contains("Systemdokumentation: recipes");
    }

    @Test
    void shouldRedirectTheDirectoryWithoutSlashSoRelativeLinksKeepWorking() {
        // Without the trailing slash the browser would resolve the diagrams against `/recipes/`.
        HttpResponse<String> response = HttpProbe.get("/docs");

        assertThat(response.statusCode()).isEqualTo(302);
        // The redirect is context-relative; the container answers with the absolute location.
        assertThat(response.headers().firstValue("Location")).hasValueSatisfying(
                location -> assertThat(location).endsWith(RunningApplication.CONTEXT_PATH + "/docs/"));
    }

    @Test
    void shouldServeEveryResourceTheDocumentationEmbeds() {
        String documentation = HttpProbe.get("/docs/").body();
        Matcher matcher = RELATIVE_RESOURCE.matcher(documentation);

        Set<String> resources = new LinkedHashSet<>();
        while (matcher.find()) {
            resources.add(matcher.group(1));
        }

        for (String resource : resources) {
            HttpResponse<String> response = HttpProbe.get("/docs/" + resource);
            assertThat(response.statusCode()).as(resource).isEqualTo(200);
            assertThat(response.body()).as(resource).isNotEmpty();
        }

        // A documentation without any embedded resource would mean the copy step lost the
        // rendered diagrams – the document would still load, only with broken images.
        assertThat(resources).as("resources embedded by the documentation").isNotEmpty();
    }

    @Test
    void shouldNotLetTheDocumentationShadowTheApplication() {
        assertThat(HttpProbe.get("/").statusCode()).as("SPA").isEqualTo(200);
        assertThat(HttpProbe.get("/api/recipes").statusCode()).as("API").isEqualTo(200);
        // Only the two mappings above exist; anything else below /docs stays a 404.
        assertThat(HttpProbe.get("/docs/gibt-es-nicht").statusCode()).isEqualTo(404);
    }
}
