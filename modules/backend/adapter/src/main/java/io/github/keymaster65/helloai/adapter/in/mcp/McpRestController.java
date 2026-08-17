package io.github.keymaster65.helloai.adapter.in.mcp;

import io.github.keymaster65.helloai.adapter.in.mcp.dto.McpResourceContentDto;
import io.github.keymaster65.helloai.adapter.in.mcp.dto.McpResourceDto;
import io.github.keymaster65.helloai.adapter.in.mcp.dto.McpServerInfoDto;
import io.github.keymaster65.helloai.adapter.in.mcp.dto.McpToolDto;
import io.github.keymaster65.helloai.adapter.in.mcp.dto.McpToolResultDto;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The MCP server as an ordinary REST API, described in the OpenAPI contract and thus usable from
 * Swagger UI (ADR 0050).
 *
 * <p>The protocol endpoint next door speaks JSON-RPC 2.0 over a servlet of its own and is therefore
 * invisible to springdoc (ADR 0049, ADR 0005). This controller closes that gap: it offers the same
 * four tools and the same resources over plain HTTP, one operation per method of the protocol
 * &ndash; {@code tools/list}, {@code tools/call}, {@code resources/list}, {@code resources/read}.
 *
 * <p>It sits <em>in the MCP adapter</em>, not next to the recipe controller: it reads the tool and
 * resource specifications of this package, and adapters do not know each other (ADR 0019).
 *
 * <p>Both fronts run the same handlers through {@link McpCatalog}, so a tool exists here exactly
 * when it exists there. What differs is only the envelope &ndash; and the {@code Origin} check of
 * the protocol transport, which a request through Spring MVC does not pass (see ADR 0050,
 * &bdquo;Sicherheit&ldquo;).
 */
@RestController
@RequestMapping(McpRestController.BASE_PATH)
@Tag(name = "MCP", description = "The read-only MCP server of this application, callable over plain HTTP")
public class McpRestController {

    /**
     * The facade lives <em>below</em> the protocol endpoint. That is not a collision: the transport
     * servlet is registered for the exact path {@code /api/mcp}, so every longer path falls through
     * to the {@code DispatcherServlet} and thus to this controller.
     */
    static final String BASE_PATH = McpServerConfig.MCP_ENDPOINT;

    /** Errors answer as RFC 9457 problem details, like everywhere else in this API (ADR 0046). */
    private static final String ERROR_MEDIA_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final McpCatalog catalog;
    private final McpStatelessSyncServer server;

    McpRestController(McpCatalog catalog, McpStatelessSyncServer server) {
        this.catalog = catalog;
        this.server = server;
    }

    @GetMapping("/server")
    @Operation(summary = "What the MCP server says about itself",
            description = """
                    Name, version and the starting hint a protocol client receives from \
                    initialize. Read this first: the instructions name the tools to begin with.""")
    @ApiResponse(responseCode = "200", description = "Name, version and instructions")
    public McpServerInfoDto server() {
        return McpServerInfoDto.curried()
                .name(server.getServerInfo().name())
                .version(server.getServerInfo().version())
                .instructions(McpServerConfig.INSTRUCTIONS);
    }

    @GetMapping("/tools")
    @Operation(summary = "List the tools of the MCP server",
            description = """
                    The same list a protocol client gets from tools/list, including the JSON \
                    Schema each tool validates its arguments against.""")
    @ApiResponse(responseCode = "200", description = "All tools, in the order a client should discover them")
    public List<McpToolDto> tools() {
        return catalog.tools().stream().map(specification -> McpRestMapper.tool(specification.tool())).toList();
    }

    @PostMapping("/tools/{name}")
    @Operation(summary = "Call a tool",
            description = """
                    Runs the tool and returns its result. The request body carries the arguments \
                    as a JSON object, exactly as the tool's inputSchema describes them; a tool \
                    without arguments takes an empty object. A failure the caller can act on – an \
                    unknown identifier, an argument the schema rejects – is answered with 200 and \
                    isError set, because the tool ran and reported it. That is how the protocol \
                    answers as well.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The result of the tool, possibly marked as an error"),
            @ApiResponse(responseCode = "404", description = "No tool carries this name",
                    content = @Content(mediaType = ERROR_MEDIA_TYPE))
    })
    public McpToolResultDto callTool(
            @Parameter(description = "Name of the tool, as returned by the tool list", example = "list_recipes")
            @PathVariable String name,
            @RequestBody(required = false) Map<String, Object> arguments) {

        return catalog.callTool(name, arguments)
                .map(McpRestMapper::result)
                .orElseThrow(() -> notFound("No tool exists with the name " + name));
    }

    @GetMapping("/resources")
    @Operation(summary = "List the resources of the MCP server",
            description = """
                    The pages of the delivered system documentation, one resource each. The list \
                    is built when the application starts and is empty in a deployable built \
                    without documentation.""")
    @ApiResponse(responseCode = "200", description = "All resources, possibly an empty list")
    public List<McpResourceDto> resources() {
        return catalog.resources().stream()
                .map(specification -> McpRestMapper.resource(specification.resource()))
                .toList();
    }

    @GetMapping("/resources/content")
    @Operation(summary = "Read a resource",
            description = "Returns the content blocks of one resource. Takes the URI from the resource list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The content of the resource"),
            @ApiResponse(responseCode = "404", description = "No resource carries this URI",
                    content = @Content(mediaType = ERROR_MEDIA_TYPE))
    })
    public List<McpResourceContentDto> resourceContent(
            @Parameter(description = "URI of the resource, as returned by the resource list",
                    example = "recipes://docs/system")
            @RequestParam String uri) {

        return catalog.readResource(uri)
                .map(result -> result.contents().stream().map(McpRestMapper::content).toList())
                .orElseThrow(() -> notFound("No resource exists with the URI " + uri));
    }

    /** Names what was asked for and nothing from inside the application (ADR 0046). */
    private static ResponseStatusException notFound(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
    }
}
