package io.github.keymaster65.helloai.systemtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * System tests for the machine-readable reporting channel (RFC 9116, see docs/prompt/security-backend.adoc): the running
 * application serves {@code /.well-known/security.txt} at its <em>origin</em>, next to the
 * context path rather than below it.
 *
 * <p>Reachability is the whole point of this file – a contact nobody finds is no contact. It is
 * therefore checked over HTTP, exactly the way a finder or a tool would ask for it.
 */
@EnabledIf("applicationAvailable")
class SecurityTxtSystemTest {

    private static final String SECURITY_TXT = "/.well-known/security.txt";

    static boolean applicationAvailable() {
        return RunningApplication.available();
    }

    @Test
    void shouldServeSecurityTxtAtTheOrigin() {
        HttpResponse<String> response = HttpProbe.getFromOrigin(SECURITY_TXT);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(contentType -> {
            String lowerCase = contentType.toLowerCase(Locale.ROOT);
            assertThat(lowerCase).contains("text/plain");
            assertThat(lowerCase).contains("utf-8");
        });
    }

    @Test
    void shouldNameTheMandatoryFields() {
        String document = HttpProbe.getFromOrigin(SECURITY_TXT).body();

        // Both fields are mandatory in RFC 9116; Expires exactly once.
        assertThat(document.lines().filter(line -> line.startsWith("Contact: "))).isNotEmpty();
        assertThat(document.lines().filter(line -> line.startsWith("Expires: "))).hasSize(1);
    }

    @Test
    void shouldNotServeSecurityTxtBelowTheContextPath() {
        // Two places for one file mean one of them ages. RFC 9116 knows the origin only.
        assertThat(HttpProbe.get(SECURITY_TXT).statusCode()).isEqualTo(404);
    }

    @Test
    void shouldLeaveEveryOtherPathUntouched() {
        // The valve answers one path and hands on the rest – including the ones that were 404
        // before it existed.
        assertThat(HttpProbe.get("/api/recipes").statusCode()).as("API").isEqualTo(200);
        assertThat(HttpProbe.get("/").statusCode()).as("SPA").isEqualTo(200);
        assertThat(HttpProbe.getFromOrigin("/.well-known/gibt-es-nicht").statusCode())
                .as("other well-known path").isEqualTo(404);
    }
}
