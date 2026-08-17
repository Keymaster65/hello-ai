package io.github.keymaster65.helloai.adapter.in.mcp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The answer of a tool call (see ADR 0050).
 *
 * <p>A failure the caller can act on &ndash; an unknown identifier, an argument that does not match
 * the schema &ndash; is <em>this</em> result with {@code isError}, not an HTTP error: the tool ran
 * and reported something. That is how the protocol answers, and the facade does not invent a second
 * story for it.
 *
 * @param content the text blocks of the result, in the order the tool produced them
 * @param isError whether the tool reported a failure instead of a result
 */
@Schema(name = "McpToolResult", description = "The answer of a tool call")
public record McpToolResultDto(
        @Schema(description = "Text blocks of the result",
                requiredMode = Schema.RequiredMode.REQUIRED) List<String> content,
        @Schema(description = "Whether the tool reported a failure instead of a result", example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean isError) {
}
