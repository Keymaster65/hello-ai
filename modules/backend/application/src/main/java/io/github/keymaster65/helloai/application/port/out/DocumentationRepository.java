package io.github.keymaster65.helloai.application.port.out;

import io.github.keymaster65.helloai.domain.model.DocumentationPage;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port: access to the pages of the delivered system documentation (ADR 0049).
 * Implemented by an outbound adapter (the classpath repository).
 */
public interface DocumentationRepository {

    /**
     * Returns all pages of the delivered documentation.
     *
     * @return all pages, possibly empty when no documentation was packed into the deployable
     */
    List<DocumentationPage> findAll();

    /**
     * Reads the content of a single page.
     *
     * @param id the identifier of the page
     * @return the content, or {@link Optional#empty()} if no page has this identifier
     */
    Optional<String> findContent(String id);
}
