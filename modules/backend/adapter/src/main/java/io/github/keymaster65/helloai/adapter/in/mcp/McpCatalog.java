package io.github.keymaster65.helloai.adapter.in.mcp;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.ToolInputValidator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * What this adapter offers, in one place: the tool and resource specifications, and the two calls
 * that run them (ADR 0049, ADR 0050).
 *
 * <p>It exists because the same catalogue now has <em>two</em> fronts &ndash; the JSON-RPC endpoint
 * assembled in {@link McpServerConfig} and the REST facade in {@link McpRestController}. Assembled
 * twice it would drift: a tool added to one front and forgotten in the other is a difference nobody
 * sees until a client asks.
 *
 * <p>The lists are built once, in the constructor. The resource catalogue of a stateless server is
 * fixed at start-up anyway (ADR 0049), and the tools are the same objects the server holds.
 */
@Component
class McpCatalog {

    /**
     * Whether arguments are checked against the tool's input schema before its handler runs. Both
     * fronts read this constant, so an argument the protocol endpoint rejects is rejected by the
     * facade too &ndash; {@code true} is also the SDK's own default for the server.
     */
    static final boolean VALIDATE_TOOL_INPUTS = true;

    private final List<SyncToolSpecification> tools;
    private final List<SyncResourceSpecification> resources;
    private final Map<String, SyncToolSpecification> toolsByName;
    private final Map<String, SyncResourceSpecification> resourcesByUri;
    private final JsonSchemaValidator schemaValidator;

    McpCatalog(
            RecipeMcpTools recipeMcpTools,
            DocumentationMcpTools documentationMcpTools,
            DocumentationMcpResources documentationMcpResources,
            JsonSchemaValidator schemaValidator) {

        this.tools = Stream.concat(
                        recipeMcpTools.specifications().stream(),
                        documentationMcpTools.specifications().stream())
                .toList();
        this.resources = documentationMcpResources.specifications();
        this.toolsByName = index(tools, specification -> specification.tool().name());
        this.resourcesByUri = index(resources, specification -> specification.resource().uri());
        this.schemaValidator = schemaValidator;
    }

    /** @return the tools, in the order a client should discover them */
    List<SyncToolSpecification> tools() {
        return tools;
    }

    /** @return the resources, one per page of the delivered documentation; empty without it */
    List<SyncResourceSpecification> resources() {
        return resources;
    }

    /**
     * Runs a tool, after checking its arguments against the input schema.
     *
     * @param name      name of the tool
     * @param arguments the arguments, {@code null} for a tool without any
     * @return the result, or {@link Optional#empty()} if no tool carries that name
     */
    Optional<McpSchema.CallToolResult> callTool(String name, Map<String, Object> arguments) {
        return Optional.ofNullable(toolsByName.get(name)).map(specification -> {
            Map<String, Object> passed = arguments == null ? Map.of() : arguments;
            McpSchema.CallToolResult rejected = ToolInputValidator.validate(
                    specification.tool(), passed, VALIDATE_TOOL_INPUTS, schemaValidator);
            return rejected != null
                    ? rejected
                    : specification.callHandler().apply(
                            McpTransportContext.EMPTY, new McpSchema.CallToolRequest(name, passed, null));
        });
    }

    /**
     * Reads a resource.
     *
     * @param uri address of the resource
     * @return the contents, or {@link Optional#empty()} if no resource carries that URI
     */
    Optional<McpSchema.ReadResourceResult> readResource(String uri) {
        return Optional.ofNullable(resourcesByUri.get(uri))
                .map(specification -> specification.readHandler().apply(
                        McpTransportContext.EMPTY, new McpSchema.ReadResourceRequest(uri, null)));
    }

    private static <T> Map<String, T> index(List<T> specifications, Function<T, String> key) {
        Map<String, T> byKey = new LinkedHashMap<>();
        specifications.forEach(specification -> byKey.put(key.apply(specification), specification));
        return Map.copyOf(byKey);
    }
}
