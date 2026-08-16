package io.github.keymaster65.helloai.domain.model;

import java.util.Objects;

/**
 * A page of the delivered system documentation.
 *
 * <p>The documentation is part of what this project ships, not a by-product of its build: it
 * travels inside the deployable and is served next to the application (ADR 0024). That is why a
 * page is a domain type and reachable through a port like any other content (ADR 0049).
 *
 * <p>The page carries no content. Titles are cheap to list, pages are not &ndash; the content is
 * fetched for one page at a time.
 *
 * @param id    stable identifier, also the last part of the page's URI (e.g. {@code system} or
 *              {@code adr/0049-mcp-service-fuer-rezepte-und-dokumentation}); must not be
 *              {@code null} or blank
 * @param title human readable title of the page, must not be {@code null} or blank
 */
public record DocumentationPage(String id, String title) {

    public DocumentationPage {
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        Objects.requireNonNull(title, "title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }
}
