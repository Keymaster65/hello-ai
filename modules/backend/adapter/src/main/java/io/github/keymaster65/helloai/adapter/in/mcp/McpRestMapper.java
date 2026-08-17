package io.github.keymaster65.helloai.adapter.in.mcp;

import io.github.keymaster65.helloai.adapter.in.mcp.dto.McpResourceContentDto;
import io.github.keymaster65.helloai.adapter.in.mcp.dto.McpResourceDto;
import io.github.keymaster65.helloai.adapter.in.mcp.dto.McpToolDto;
import io.github.keymaster65.helloai.adapter.in.mcp.dto.McpToolResultDto;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;

/**
 * Turns the types of the MCP SDK into the DTOs of the REST facade (ADR 0050).
 *
 * <p>This translation is the reason the facade has DTOs at all: the SDK types carry fields the
 * protocol needs and a caller of the REST API does not &ndash; {@code _meta}, annotations, icons.
 * They are also types of a library that must not appear in the OpenAPI contract, which would tie
 * the published schema to the SDK's release cadence.
 */
final class McpRestMapper {

    private McpRestMapper() {
    }

    /**
     * @param tool a tool of the server
     * @return the tool as the facade shows it
     */
    static McpToolDto tool(McpSchema.Tool tool) {
        return McpToolDto.curried()
                .name(tool.name())
                .title(tool.title())
                .description(tool.description())
                .inputSchema(tool.inputSchema() == null ? Map.of() : tool.inputSchema());
    }

    /**
     * @param result the answer of a tool call
     * @return the answer as the facade shows it, with the text blocks in their original order
     */
    static McpToolResultDto result(McpSchema.CallToolResult result) {
        List<String> content = result.content() == null
                ? List.of()
                : result.content().stream().map(McpRestMapper::text).toList();
        return new McpToolResultDto(content, Boolean.TRUE.equals(result.isError()));
    }

    /**
     * @param resource a resource of the server
     * @return the resource as the facade shows it
     */
    static McpResourceDto resource(McpSchema.Resource resource) {
        return McpResourceDto.curried()
                .uri(resource.uri())
                .name(resource.name())
                .title(resource.title())
                .description(resource.description())
                .mimeType(resource.mimeType());
    }

    /**
     * @param contents one content block of a resource
     * @return the block as the facade shows it; {@code text} stays {@code null} for a binary block
     */
    static McpResourceContentDto content(McpSchema.ResourceContents contents) {
        return McpResourceContentDto.curried()
                .uri(contents.uri())
                .mimeType(contents.mimeType())
                .text(contents instanceof McpSchema.TextResourceContents text ? text.text() : null);
    }

    /**
     * The text of a content block. Every tool of this adapter answers with text (ADR 0049); a block
     * of another kind is named by its type instead of being dropped.
     */
    private static String text(McpSchema.Content content) {
        return content instanceof McpSchema.TextContent text ? text.text() : "[" + content.type() + "]";
    }
}
