package io.github.keymaster65.helloai.adapter.in.mcp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The content of one resource (see ADR 0050).
 *
 * <p>{@code text} stays {@code null} for a content that is not text. This server has no such
 * resource &ndash; every page of the documentation is HTML (ADR 0024) &ndash; but dropping the
 * entry silently would be worse than an empty field.
 *
 * @param uri      address the content was read from
 * @param mimeType media type of the content
 * @param text     the content itself, {@code null} if it is not text
 */
@Schema(name = "McpResourceContent", description = "The content of one resource")
public record McpResourceContentDto(
        @Schema(description = "Address the content was read from", example = "recipes://docs/system",
                requiredMode = Schema.RequiredMode.REQUIRED) String uri,
        @Schema(description = "Media type of the content", example = "text/html") String mimeType,
        @Schema(description = "The content itself, absent if it is not text") String text) {

    /**
     * Starts the curried construction of a {@link McpResourceContentDto} (see ADR 0021).
     *
     * @return the first step of the curried factory
     */
    public static UriStep curried() {
        return uri -> mimeType -> text -> new McpResourceContentDto(uri, mimeType, text);
    }

    /** Step 1 of {@link #curried()}: the URI. */
    @FunctionalInterface
    public interface UriStep {

        /**
         * @param uri address the content was read from
         * @return the next step
         */
        MimeTypeStep uri(String uri);
    }

    /** Step 2 of {@link #curried()}: the media type. */
    @FunctionalInterface
    public interface MimeTypeStep {

        /**
         * @param mimeType media type of the content
         * @return the next step
         */
        TextStep mimeType(String mimeType);
    }

    /** Step 3 of {@link #curried()}: the text, completing the content. */
    @FunctionalInterface
    public interface TextStep {

        /**
         * @param text the content itself, {@code null} if it is not text
         * @return the finished {@link McpResourceContentDto}
         */
        McpResourceContentDto text(String text);
    }
}
