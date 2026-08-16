package io.github.keymaster65.helloai.adapter.in.mcp;

import io.github.keymaster65.helloai.application.port.in.DocumentationService;
import io.github.keymaster65.helloai.domain.model.DocumentationPage;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The pages of the delivered system documentation as MCP resources (ADR 0049).
 *
 * <p>Resources are the form the protocol has for content a client attaches instead of fetching:
 * they carry a URI, a title and a media type. That fits the documentation, which is packed into
 * the deployable and does not change while the application runs (ADR 0024) &ndash; the catalogue
 * of a stateless server is built once, at start-up.
 *
 * <p>The same pages are additionally reachable as tools ({@link DocumentationMcpTools}), because
 * not every client supports resources.
 */
@Component
class DocumentationMcpResources {

    /** Scheme and path of a page; the identifier of the page completes it. */
    static final String URI_PREFIX = "recipes://docs/";

    /** What the build delivers: the rendered documentation (ADR 0024, ADR 0026). */
    static final String MEDIA_TYPE = "text/html";

    private final DocumentationService documentationService;

    DocumentationMcpResources(DocumentationService documentationService) {
        this.documentationService = documentationService;
    }

    /**
     * The resource specifications this adapter contributes to the server, one per page.
     *
     * @return the specifications, empty if the deployable carries no documentation
     */
    List<SyncResourceSpecification> specifications() {
        return documentationService.getAll().stream().map(this::specification).toList();
    }

    /**
     * The URI under which a page is offered.
     *
     * @param id identifier of the page
     * @return the URI of the page
     */
    static String uri(String id) {
        return URI_PREFIX + id;
    }

    private SyncResourceSpecification specification(DocumentationPage page) {
        McpSchema.Resource resource = McpSchema.Resource.builder(uri(page.id()), page.id())
                .title(page.title())
                .description("Page of the system documentation of this application: " + page.title())
                .mimeType(MEDIA_TYPE)
                .build();

        return new SyncResourceSpecification(resource, (context, request) -> contents(page, request.uri()));
    }

    private McpSchema.ReadResourceResult contents(DocumentationPage page, String uri) {
        McpSchema.TextResourceContents contents = McpSchema.TextResourceContents
                .builder(uri, documentationService.getContent(page.id()))
                .mimeType(MEDIA_TYPE)
                .build();
        return McpSchema.ReadResourceResult.builder(List.of(contents)).build();
    }
}
