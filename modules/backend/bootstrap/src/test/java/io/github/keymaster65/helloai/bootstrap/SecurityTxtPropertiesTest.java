package io.github.keymaster65.helloai.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The rendered {@code security.txt} – the part of docs/prompt/security-backend.adoc that can be checked without a running
 * container. That it is actually reachable at the origin is the job of {@code
 * SecurityTxtSystemTest}.
 */
class SecurityTxtPropertiesTest {

    private static final OffsetDateTime EXPIRES = OffsetDateTime.parse("2027-08-01T00:00:00Z");

    @Test
    void shouldRenderOneContactLinePerChannelInOrderOfPreference() {
        SecurityTxtProperties properties = SecurityTxtProperties.curried()
                .contact(List.of(URI.create("mailto:security@example.org"),
                        URI.create("https://example.org/report")))
                .expires(EXPIRES)
                .preferredLanguages(List.of());

        String document = properties.document();

        assertThat(document.lines().filter(line -> line.startsWith("Contact: ")))
                .containsExactly("Contact: mailto:security@example.org",
                        "Contact: https://example.org/report");
    }

    @Test
    void shouldRenderExpiresExactlyOnceAsRfc3339Timestamp() {
        SecurityTxtProperties properties = properties(List.of("de", "en"));

        String document = properties.document();

        assertThat(document.lines().filter(line -> line.startsWith("Expires: ")))
                .containsExactly("Expires: 2027-08-01T00:00:00Z");
    }

    @Test
    void shouldRenderPreferredLanguagesAsSingleLine() {
        SecurityTxtProperties properties = properties(List.of("de", "en"));

        String document = properties.document();

        // RFC 9116 allows the field at most once; the languages are separated inside it.
        assertThat(document.lines().filter(line -> line.startsWith("Preferred-Languages: ")))
                .containsExactly("Preferred-Languages: de, en");
    }

    @Test
    void shouldOmitPreferredLanguagesWhenNoneAreConfigured() {
        SecurityTxtProperties properties = properties(List.of());

        assertThat(properties.document()).doesNotContain("Preferred-Languages");
    }

    @Test
    void shouldSeparateLinesWithCrlf() {
        String document = properties(List.of("de")).document();

        assertThat(document).endsWith("\r\n");
        // No bare LF: every line ends with CRLF, as RFC 9116 asks for.
        assertThat(document.replace("\r\n", "")).doesNotContain("\n");
    }

    @Test
    void shouldRejectAnEmptyContactList() {
        assertThatThrownBy(() -> new SecurityTxtProperties(List.of(), EXPIRES, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contact");
    }

    @Test
    void shouldTreatMissingPreferredLanguagesAsNone() {
        SecurityTxtProperties properties =
                new SecurityTxtProperties(List.of(URI.create("mailto:security@example.org")), EXPIRES, null);

        assertThat(properties.preferredLanguages()).isEmpty();
    }

    private static SecurityTxtProperties properties(List<String> preferredLanguages) {
        return SecurityTxtProperties.curried()
                .contact(List.of(URI.create("mailto:security@example.org")))
                .expires(EXPIRES)
                .preferredLanguages(preferredLanguages);
    }
}
