package io.github.keymaster65.helloai.adapter.in.mcp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What the MCP server says about itself &ndash; the answer a protocol client gets from
 * {@code initialize}, without the parts that only concern the protocol (see ADR 0050).
 *
 * @param name         name the server reports, identical to the application name
 * @param version      version the server reports
 * @param instructions where a caller should start; the same text a protocol client receives
 */
@Schema(name = "McpServerInfo", description = "Name, version and starting hint of the MCP server")
public record McpServerInfoDto(
        @Schema(description = "Name the server reports", example = "recipes",
                requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(description = "Version the server reports", example = "0.0.1-SNAPSHOT",
                requiredMode = Schema.RequiredMode.REQUIRED) String version,
        @Schema(description = "Where a caller should start, as sent to a protocol client",
                requiredMode = Schema.RequiredMode.REQUIRED) String instructions) {

    /**
     * Starts the curried construction of a {@link McpServerInfoDto} (see ADR 0021).
     *
     * @return the first step of the curried factory
     */
    public static NameStep curried() {
        return name -> version -> instructions -> new McpServerInfoDto(name, version, instructions);
    }

    /** Step 1 of {@link #curried()}: the name. */
    @FunctionalInterface
    public interface NameStep {

        /**
         * @param name name the server reports
         * @return the next step
         */
        VersionStep name(String name);
    }

    /** Step 2 of {@link #curried()}: the version. */
    @FunctionalInterface
    public interface VersionStep {

        /**
         * @param version version the server reports
         * @return the next step
         */
        InstructionsStep version(String version);
    }

    /** Step 3 of {@link #curried()}: the instructions, completing the response. */
    @FunctionalInterface
    public interface InstructionsStep {

        /**
         * @param instructions where a caller should start
         * @return the finished {@link McpServerInfoDto}
         */
        McpServerInfoDto instructions(String instructions);
    }
}
