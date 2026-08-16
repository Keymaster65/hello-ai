package io.github.keymaster65.helloai.application.port.in;

import io.github.keymaster65.helloai.domain.model.DocumentationPage;
import java.util.List;

/**
 * Inbound port: read access to the delivered system documentation (ADR 0049). Implemented by the
 * application layer and driven by inbound adapters (currently the MCP adapter).
 */
public interface DocumentationService {

    /**
     * Returns the pages of the delivered documentation.
     *
     * @return all pages, possibly empty when no documentation was packed into the deployable
     */
    List<DocumentationPage> getAll();

    /**
     * Returns the content of a single page.
     *
     * @param id the identifier of the page
     * @return the content of the page
     * @throws io.github.keymaster65.helloai.application.service.DocumentationNotFoundException
     *         if no page with {@code id} exists
     */
    String getContent(String id);
}
