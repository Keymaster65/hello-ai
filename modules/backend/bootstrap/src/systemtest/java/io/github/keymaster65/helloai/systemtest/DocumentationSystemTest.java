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
 * System tests for the system documentation shipped inside the deployable (see ADR 0024): the
 * running application serves the rendered HTML together with the ADRs it links to.
 *
 * <p>These tests fail – deliberately – when the jar was built with {@code -PskipDocs}: the flag
 * buys a fast inner loop, not a documented deployable.
 */
@EnabledIf("applicationAvailable")
class DocumentationSystemTest {

    /**
     * The document links its ADRs relatively (attribute {@code adr} = {@code adr}), so every hit
     * is a path below {@code /docs/}.
     */
    private static final Pattern ADR_REFERENCE = Pattern.compile("href=\"(adr/[^\"]+)\"");

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
        // Without the trailing slash the browser would resolve `adr/…` against `/recipes/`.
        HttpResponse<String> response = HttpProbe.get("/docs");

        assertThat(response.statusCode()).isEqualTo(302);
        // The redirect is context-relative; the container answers with the absolute location.
        assertThat(response.headers().firstValue("Location")).hasValueSatisfying(
                location -> assertThat(location).endsWith(RunningApplication.CONTEXT_PATH + "/docs/"));
    }

    @Test
    void shouldServeEveryAdrTheDocumentationLinksTo() {
        String documentation = HttpProbe.get("/docs/").body();
        Matcher matcher = ADR_REFERENCE.matcher(documentation);

        Set<String> adrs = new LinkedHashSet<>();
        while (matcher.find()) {
            adrs.add(matcher.group(1));
        }

        for (String adr : adrs) {
            HttpResponse<String> response = HttpProbe.get("/docs/" + adr);
            assertThat(response.statusCode()).as(adr).isEqualTo(200);
            // Since ADR 0026 the ADRs ship as rendered HTML, not as source text.
            assertThat(response.headers().firstValue("Content-Type")).as(adr)
                    .hasValueSatisfying(contentType -> assertThat(contentType).contains("text/html"));
            // Its own number in the document title – enough to tell the document apart from an
            // error page, and blind to the two title styles the ADRs grew over time.
            assertThat(response.body()).as(adr)
                    .contains("<h1>")
                    .contains(nummer(adr));
        }

        // A documentation without ADR links would mean the attribute was not applied – the links
        // would then point back into the repository and break wherever the jar runs.
        assertThat(adrs).as("ADRs referenced by the documentation").isNotEmpty();
    }

    /** {@code adr/0022-asciidoc-….html} → {@code 0022}. */
    private static String nummer(String adr) {
        String dateiname = adr.substring(adr.lastIndexOf('/') + 1);
        return dateiname.substring(0, dateiname.indexOf('-'));
    }

    @Test
    void shouldNotLetTheDocumentationShadowTheApplication() {
        assertThat(HttpProbe.get("/").statusCode()).as("SPA").isEqualTo(200);
        assertThat(HttpProbe.get("/api/recipes").statusCode()).as("API").isEqualTo(200);
        // Only the two mappings above exist; anything else below /docs stays a 404.
        assertThat(HttpProbe.get("/docs/gibt-es-nicht").statusCode()).isEqualTo(404);
    }
}
