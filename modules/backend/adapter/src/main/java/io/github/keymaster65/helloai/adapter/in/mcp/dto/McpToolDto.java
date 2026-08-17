package io.github.keymaster65.helloai.adapter.in.mcp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * One tool of the MCP server as the REST facade shows it (see ADR 0050).
 *
 * <p>{@code inputSchema} is passed through unchanged: it is the JSON Schema the server validates a
 * call against, and it is what tells a caller which arguments to send. Describing it a second time
 * in OpenAPI would be a copy that ages.
 *
 * @param name        identifier of the tool, used in the path of a call
 * @param title       short human-readable title
 * @param description what the tool does, as a language model reads it
 * @param inputSchema JSON Schema of the arguments the tool accepts
 */
@Schema(name = "McpTool", description = "A tool offered by the MCP server")
public record McpToolDto(
        @Schema(description = "Identifier of the tool", example = "get_recipe",
                requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(description = "Short human-readable title", example = "Get a recipe") String title,
        @Schema(description = "What the tool does") String description,
        @Schema(description = "JSON Schema of the arguments the tool accepts",
                requiredMode = Schema.RequiredMode.REQUIRED) Map<String, Object> inputSchema) {

    /**
     * Starts the curried construction of a {@link McpToolDto} (see ADR 0021).
     *
     * @return the first step of the curried factory
     */
    public static NameStep curried() {
        return name -> title -> description -> inputSchema ->
                new McpToolDto(name, title, description, inputSchema);
    }

    /** Step 1 of {@link #curried()}: the name. */
    @FunctionalInterface
    public interface NameStep {

        /**
         * @param name identifier of the tool
         * @return the next step
         */
        TitleStep name(String name);
    }

    /** Step 2 of {@link #curried()}: the title. */
    @FunctionalInterface
    public interface TitleStep {

        /**
         * @param title short human-readable title
         * @return the next step
         */
        DescriptionStep title(String title);
    }

    /** Step 3 of {@link #curried()}: the description. */
    @FunctionalInterface
    public interface DescriptionStep {

        /**
         * @param description what the tool does
         * @return the next step
         */
        InputSchemaStep description(String description);
    }

    /** Step 4 of {@link #curried()}: the input schema, completing the tool. */
    @FunctionalInterface
    public interface InputSchemaStep {

        /**
         * @param inputSchema JSON Schema of the arguments the tool accepts
         * @return the finished {@link McpToolDto}
         */
        McpToolDto inputSchema(Map<String, Object> inputSchema);
    }
}
