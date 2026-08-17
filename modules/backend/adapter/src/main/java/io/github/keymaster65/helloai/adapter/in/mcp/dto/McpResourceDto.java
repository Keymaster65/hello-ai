package io.github.keymaster65.helloai.adapter.in.mcp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One resource of the MCP server as the REST facade shows it (see ADR 0050): a page of the
 * delivered system documentation.
 *
 * @param uri         address of the resource, the argument for reading it
 * @param name        identifier of the page
 * @param title       title of the page, taken from its HTML head
 * @param description one sentence about the page
 * @param mimeType    media type of the content
 */
@Schema(name = "McpResource", description = "A resource offered by the MCP server")
public record McpResourceDto(
        @Schema(description = "Address of the resource", example = "recipes://docs/system",
                requiredMode = Schema.RequiredMode.REQUIRED) String uri,
        @Schema(description = "Identifier of the page", example = "system",
                requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(description = "Title of the page") String title,
        @Schema(description = "One sentence about the page") String description,
        @Schema(description = "Media type of the content", example = "text/html") String mimeType) {

    /**
     * Starts the curried construction of a {@link McpResourceDto} (see ADR 0021).
     *
     * @return the first step of the curried factory
     */
    public static UriStep curried() {
        return uri -> name -> title -> description -> mimeType ->
                new McpResourceDto(uri, name, title, description, mimeType);
    }

    /** Step 1 of {@link #curried()}: the URI. */
    @FunctionalInterface
    public interface UriStep {

        /**
         * @param uri address of the resource
         * @return the next step
         */
        NameStep uri(String uri);
    }

    /** Step 2 of {@link #curried()}: the name. */
    @FunctionalInterface
    public interface NameStep {

        /**
         * @param name identifier of the page
         * @return the next step
         */
        TitleStep name(String name);
    }

    /** Step 3 of {@link #curried()}: the title. */
    @FunctionalInterface
    public interface TitleStep {

        /**
         * @param title title of the page
         * @return the next step
         */
        DescriptionStep title(String title);
    }

    /** Step 4 of {@link #curried()}: the description. */
    @FunctionalInterface
    public interface DescriptionStep {

        /**
         * @param description one sentence about the page
         * @return the next step
         */
        MimeTypeStep description(String description);
    }

    /** Step 5 of {@link #curried()}: the media type, completing the resource. */
    @FunctionalInterface
    public interface MimeTypeStep {

        /**
         * @param mimeType media type of the content
         * @return the finished {@link McpResourceDto}
         */
        McpResourceDto mimeType(String mimeType);
    }
}
