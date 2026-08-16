package io.github.keymaster65.helloai.adapter.in.mcp;

import static io.github.keymaster65.helloai.adapter.in.mcp.McpToolResults.NO_ARGUMENTS;
import static io.github.keymaster65.helloai.adapter.in.mcp.McpToolResults.failure;
import static io.github.keymaster65.helloai.adapter.in.mcp.McpToolResults.json;
import static io.github.keymaster65.helloai.adapter.in.mcp.McpToolResults.textArgument;

import io.github.keymaster65.helloai.application.port.in.DocumentationService;
import io.github.keymaster65.helloai.application.service.DocumentationNotFoundException;
import io.github.keymaster65.helloai.domain.model.DocumentationPage;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The delivered system documentation as MCP tools (ADR 0049): {@code list_documentation} and
 * {@code read_documentation}.
 *
 * <p>The same pages are offered as resources ({@link DocumentationMcpResources}). The duplication
 * is deliberate: a client that only supports tools would otherwise not see the documentation at
 * all. Both ways read through the same {@link DocumentationService} port, so there is one source.
 */
@Component
class DocumentationMcpTools {

    private static final String LIST_TOOL = "list_documentation";
    private static final String READ_TOOL = "read_documentation";
    private static final String ID_ARGUMENT = "id";

    private static final Map<String, Object> ID_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    ID_ARGUMENT, Map.of(
                            "type", "string",
                            "minLength", 1,
                            "description", "Identifier of the page, as returned by list_documentation")),
            "required", List.of(ID_ARGUMENT),
            "additionalProperties", false);

    private final DocumentationService documentationService;
    private final McpJsonMapper jsonMapper;

    DocumentationMcpTools(DocumentationService documentationService, McpJsonMapper jsonMapper) {
        this.documentationService = documentationService;
        this.jsonMapper = jsonMapper;
    }

    /**
     * The tool specifications this adapter contributes to the server.
     *
     * @return the specifications, in the order a client should discover them
     */
    List<SyncToolSpecification> specifications() {
        return List.of(listDocumentation(), readDocumentation());
    }

    private SyncToolSpecification listDocumentation() {
        McpSchema.Tool tool = McpSchema.Tool.builder(LIST_TOOL, NO_ARGUMENTS)
                .title("List documentation pages")
                .description("""
                        Lists the pages of the system documentation of this application: the \
                        chapters and one page per architecture decision. Each page has an \
                        identifier for read_documentation and the URI it is also offered under as \
                        an MCP resource.""")
                .annotations(readOnly())
                .build();

        return SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((context, request) -> json(
                        jsonMapper,
                        documentationService.getAll().stream().map(DocumentationMcpTools::page).toList()))
                .build();
    }

    private SyncToolSpecification readDocumentation() {
        McpSchema.Tool tool = McpSchema.Tool.builder(READ_TOOL, ID_SCHEMA)
                .title("Read a documentation page")
                .description("""
                        Returns one page of the system documentation as HTML, the form in which it \
                        is delivered. Takes the identifier from list_documentation.""")
                .annotations(readOnly())
                .build();

        return SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((context, request) -> textArgument(request, ID_ARGUMENT)
                        .map(this::content)
                        .orElseGet(() -> failure("The argument id is required and must be a non-empty string")))
                .build();
    }

    private McpSchema.CallToolResult content(String id) {
        try {
            return McpSchema.CallToolResult.builder()
                    .addTextContent(documentationService.getContent(id))
                    .build();
        } catch (DocumentationNotFoundException e) {
            return failure("No documentation page exists with the identifier " + e.id());
        }
    }

    /** Reading, and the set of pages is closed &ndash; it is fixed when the application starts. */
    private static McpSchema.ToolAnnotations readOnly() {
        return McpSchema.ToolAnnotations.builder()
                .readOnlyHint(true)
                .openWorldHint(false)
                .build();
    }

    private static Map<String, Object> page(DocumentationPage page) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", page.id());
        json.put("title", page.title());
        json.put("uri", DocumentationMcpResources.uri(page.id()));
        return json;
    }
}
