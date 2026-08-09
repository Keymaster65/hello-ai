package io.github.keymaster65.helloai.systemtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * System tests for the BFF setup (see ADR 0007): the running application serves the React bundle
 * and the API from the same origin, so the frontend needs no CORS configuration and no second
 * deployment.
 */
@EnabledIf("applicationAvailable")
class FrontendSystemTest {

    /**
     * The bundle references its assets absolutely from the origin, i.e. including the context
     * path (`/recipes/assets/…`). Only the part behind the context path is captured, because
     * {@link HttpProbe} already prefixes it.
     */
    private static final Pattern ASSET_REFERENCE = Pattern.compile(
            "(?:src|href)=\"" + Pattern.quote(RunningApplication.CONTEXT_PATH) + "(/assets/[^\"]+)\"");

    static boolean applicationAvailable() {
        return RunningApplication.available();
    }

    @Test
    void shouldServeTheSinglePageApplicationAtTheRoot() {
        HttpResponse<String> response = HttpProbe.get("/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                contentType -> assertThat(contentType).contains("text/html"));
        assertThat(response.body()).contains("<div id=\"root\">");
    }

    @Test
    void shouldServeEveryAssetReferencedByTheIndexPage() {
        String indexHtml = HttpProbe.get("/").body();
        Matcher matcher = ASSET_REFERENCE.matcher(indexHtml);

        int assets = 0;
        while (matcher.find()) {
            String asset = matcher.group(1);
            HttpResponse<String> response = HttpProbe.get(asset);
            assertThat(response.statusCode()).as(asset).isEqualTo(200);
            assertThat(response.body()).as(asset).isNotEmpty();
            assets++;
        }

        // A bundle without JS would mean the frontend build did not make it into the artifact.
        assertThat(assets).as("assets referenced by index.html").isPositive();
    }

    @Test
    void shouldServeApiAndFrontendFromTheSameOrigin() {
        // The whole point of the BFF: identical origin, so the SPA can use relative /api paths.
        assertThat(HttpProbe.get("/").statusCode()).isEqualTo(200);
        assertThat(HttpProbe.get("/api/recipes").statusCode()).isEqualTo(200);
    }
}
