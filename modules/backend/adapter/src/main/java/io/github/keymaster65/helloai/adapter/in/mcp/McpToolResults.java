package io.github.keymaster65.helloai.adapter.in.mcp;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * The three things every tool of this adapter needs: a schema for a tool without arguments, a
 * result that carries JSON, and a result that reports a failure (ADR 0049).
 *
 * <p>The failure text follows the same rule as the problem details of the REST API (ADR 0046): it
 * names the identifier the caller asked for and nothing about the inside of the application.
 */
final class McpToolResults {

    /**
     * Input schema of a tool that takes no arguments. {@code additionalProperties: false} lets the
     * SDK reject a call that passes some anyway, instead of ignoring it silently.
     */
    static final Map<String, Object> NO_ARGUMENTS = Map.of(
            "type", "object",
            "properties", Map.of(),
            "additionalProperties", false);

    private McpToolResults() {
    }

    /**
     * Serialises a value and returns it as the single text content of a successful result.
     *
     * @param jsonMapper the mapper of the MCP runtime
     * @param value      the value to serialise
     * @return the result, or a failure result if the value cannot be serialised
     */
    static McpSchema.CallToolResult json(McpJsonMapper jsonMapper, Object value) {
        try {
            return McpSchema.CallToolResult.builder()
                    .addTextContent(jsonMapper.writeValueAsString(value))
                    .build();
        } catch (IOException e) {
            return failure("The result cannot be serialised as JSON");
        }
    }

    /**
     * Reports a failure the caller can act on &ndash; an unknown identifier, a missing argument.
     *
     * @param message what went wrong, without internals
     * @return a result marked as an error
     */
    static McpSchema.CallToolResult failure(String message) {
        return McpSchema.CallToolResult.builder()
                .addTextContent(message)
                .isError(true)
                .build();
    }

    /**
     * Reads a whole-number argument. The SDK validates the arguments against the input schema
     * before the handler runs; this is the second net, so a handler never unwraps blindly.
     *
     * @param request the tool call
     * @param name    name of the argument
     * @return the value, or {@link Optional#empty()} if it is absent or not a number
     */
    static Optional<Long> longArgument(McpSchema.CallToolRequest request, String name) {
        Map<String, Object> arguments = request.arguments();
        if (arguments == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(arguments.get(name))
                .filter(Number.class::isInstance)
                .map(value -> ((Number) value).longValue());
    }

    /**
     * Reads a text argument.
     *
     * @param request the tool call
     * @param name    name of the argument
     * @return the value, or {@link Optional#empty()} if it is absent, not a string or blank
     */
    static Optional<String> textArgument(McpSchema.CallToolRequest request, String name) {
        Map<String, Object> arguments = request.arguments();
        if (arguments == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(arguments.get(name))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(value -> !value.isBlank());
    }
}
