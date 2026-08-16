package io.github.keymaster65.helloai.bootstrap;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Content of the {@code security.txt} the application serves at its origin (RFC 9116, see
 * docs/prompt/security-backend.adoc). The record renders the document itself, so field names
 * and their order live in one place.
 *
 * <p>Configured under {@code recipes.security-txt} – the reporting channel and the expiry date
 * change without a new artifact, like the database credentials next to them.
 *
 * @param contact            reporting channels as URIs ({@code mailto:}, {@code tel:} or
 *                           {@code https:}), most preferred first; must not be empty
 * @param expires            point in time after which the file is no longer valid; RFC 9116
 *                           recommends less than a year ahead
 * @param preferredLanguages languages a report is accepted in, as language tags; may be empty
 */
@ConfigurationProperties(prefix = "recipes.security-txt")
public record SecurityTxtProperties(
        List<URI> contact,
        OffsetDateTime expires,
        List<String> preferredLanguages) {

    /** Line separator of the file. RFC 9116 asks for CRLF, as every other text format on the wire. */
    private static final String CRLF = "\r\n";

    public SecurityTxtProperties {
        Objects.requireNonNull(contact, "contact must not be null");
        Objects.requireNonNull(expires, "expires must not be null");
        if (contact.isEmpty()) {
            // A security.txt without a reporting channel is not one - RFC 9116 makes the field
            // mandatory, and a file that names no way to reach anybody is worse than none.
            throw new IllegalArgumentException("security.txt needs at least one Contact URI");
        }
        contact = List.copyOf(contact);
        preferredLanguages = preferredLanguages == null ? List.of() : List.copyOf(preferredLanguages);
    }

    /**
     * Starts the curried construction of {@link SecurityTxtProperties} (see docs/prompt/architektur.adoc).
     *
     * <pre>{@code
     * SecurityTxtProperties properties = SecurityTxtProperties.curried()
     *         .contact(List.of(URI.create("https://example.org/report")))
     *         .expires(OffsetDateTime.parse("2027-08-01T00:00:00Z"))
     *         .preferredLanguages(List.of("de", "en"));
     * }</pre>
     *
     * @return the first step of the curried factory
     */
    public static ContactStep curried() {
        return contact -> expires -> languages -> new SecurityTxtProperties(contact, expires, languages);
    }

    /** Step 1 of {@link #curried()}: the reporting channels. */
    @FunctionalInterface
    public interface ContactStep {

        /**
         * @param contact reporting channels as URIs, most preferred first; must not be empty
         * @return the next step
         */
        ExpiresStep contact(List<URI> contact);
    }

    /** Step 2 of {@link #curried()}: the expiry date. */
    @FunctionalInterface
    public interface ExpiresStep {

        /**
         * @param expires point in time after which the file is no longer valid
         * @return the next step
         */
        PreferredLanguagesStep expires(OffsetDateTime expires);
    }

    /** Step 3 of {@link #curried()}: the accepted languages, completing the properties. */
    @FunctionalInterface
    public interface PreferredLanguagesStep {

        /**
         * @param preferredLanguages languages a report is accepted in; may be empty
         * @return the finished {@link SecurityTxtProperties}
         */
        SecurityTxtProperties preferredLanguages(List<String> preferredLanguages);
    }

    /**
     * Renders the file as RFC 9116 asks for it: one field per line, {@code Contact} once per
     * channel in order of preference, {@code Expires} exactly once as an RFC 3339 timestamp and
     * {@code Preferred-Languages} at most once.
     *
     * <p>Field names and comments stay English; the languages are named in the field, not in the
     * prose around it.
     *
     * @return the content of {@code /.well-known/security.txt}
     */
    public String document() {
        StringBuilder document = new StringBuilder();
        document.append("# Please report security issues in this application here.").append(CRLF);
        contact.forEach(uri -> document.append("Contact: ").append(uri).append(CRLF));
        document.append("Expires: ").append(expires.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append(CRLF);
        if (!preferredLanguages.isEmpty()) {
            document.append("Preferred-Languages: ")
                    .append(preferredLanguages.stream().map(String::trim).collect(Collectors.joining(", ")))
                    .append(CRLF);
        }
        return document.toString();
    }
}
