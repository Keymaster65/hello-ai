package io.github.keymaster65.helloai.adapter.in.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assembles the MCP server from the tools and resources of this package (ADR 0049).
 *
 * <p>It sits <em>in the adapter</em>, not in the composition root, and that is not a shortcut: the
 * onion rule says adapters do not know each other (ADR 0019), so {@code :bootstrap} &ndash; itself
 * the outermost adapter ring &ndash; must not reach for the classes here. An inbound adapter that
 * brings its own endpoint is the same shape as the REST adapter, which carries its path in
 * {@code @RequestMapping} rather than having it wired from outside.
 *
 * <p>The transport is a servlet of its own, registered next to the {@code DispatcherServlet}, so
 * requests to the endpoint bypass Spring MVC entirely. That also means springdoc cannot see it
 * (ADR 0005); {@code docs/system/api.adoc} describes it, and {@link McpRestController} offers the
 * same catalogue as operations that springdoc <em>can</em> see (ADR 0050).
 *
 * <p>Stateless streamable HTTP: one request, one response, no session. This server never sends a
 * message of its own, so there is nothing a session would be needed for.
 */
@Configuration
class McpServerConfig {

    /**
     * The endpoint, below the context path {@code /recipes} (ADR 0016) and next to the REST API:
     * it is the same programming interface, for a different caller.
     */
    static final String MCP_ENDPOINT = "/api/mcp";

    /**
     * What a client is told about this server before it lists anything. Kept short on purpose: it
     * says where to start, not what the tools do &ndash; that is the tools' own description.
     */
    static final String INSTRUCTIONS = """
            Read-only access to the recipes of this application and to its system documentation. \
            Start with list_recipes or list_documentation; both return identifiers that get_recipe \
            and read_documentation take. The documentation pages are additionally offered as \
            resources under recipes://docs/. Creating, changing and deleting recipes is not \
            possible here; that is what the REST API under /recipes/api/recipes is for.""";

    private final String applicationName;
    private final String applicationVersion;

    McpServerConfig(
            @Value("${spring.application.name}") String applicationName,
            @Value("${spring.application.version:0.0.1-SNAPSHOT}") String applicationVersion) {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    /**
     * The JSON mapper of the MCP runtime, resolved through the SDK's service loader. It is a bean
     * so that the tools get it injected instead of reaching for a static.
     */
    @Bean
    McpJsonMapper mcpJsonMapper() {
        return McpJsonDefaults.getMapper();
    }

    /**
     * The schema validator of the MCP runtime, resolved through the SDK's service loader like the
     * mapper above. It is a bean because the REST facade validates with it too (ADR 0050) &ndash;
     * an argument the protocol endpoint rejects is rejected there as well.
     */
    @Bean
    JsonSchemaValidator mcpSchemaValidator() {
        return McpJsonDefaults.getSchemaValidator();
    }

    /**
     * The transport. It gets a security validator <em>without</em> allowed origins: a request that
     * carries an {@code Origin} header is answered with 403, which is the protection against DNS
     * rebinding the MCP specification asks for. A tool without a browser context does not send the
     * header and is unaffected.
     */
    @Bean
    HttpServletStatelessServerTransport mcpTransport(McpJsonMapper mcpJsonMapper) {
        return HttpServletStatelessServerTransport.builder()
                .jsonMapper(mcpJsonMapper)
                .messageEndpoint(MCP_ENDPOINT)
                .securityValidator(DefaultServerTransportSecurityValidator.builder().build())
                .build();
    }

    /**
     * Maps the transport to its path. Without this registration Spring Boot would map the servlet
     * bean to {@code /} and it would take over the application.
     */
    @Bean
    ServletRegistrationBean<HttpServletStatelessServerTransport> mcpServletRegistration(
            HttpServletStatelessServerTransport mcpTransport) {
        ServletRegistrationBean<HttpServletStatelessServerTransport> registration =
                new ServletRegistrationBean<>(mcpTransport, MCP_ENDPOINT);
        registration.setName("mcpTransport");
        registration.setAsyncSupported(true);
        return registration;
    }

    /**
     * The server itself: four read-only tools and one resource per documentation page. Building it
     * hands the request handler to the transport, so this bean must exist for the endpoint to
     * answer at all.
     *
     * <p>{@code immediateExecution(true)} keeps the handlers on the request thread of the servlet
     * instead of moving them to a scheduler. The work is blocking anyway &ndash; a database query,
     * a classpath read &ndash; and the transaction boundary of the use cases stays where it began.
     *
     * <p>Tools and resources come from {@link McpCatalog}, the one place they are assembled: the
     * REST facade reads the same lists, and a catalogue built twice would drift (ADR 0050). Mapper,
     * validator and {@code validateToolInputs} are passed explicitly for the same reason &ndash;
     * both fronts must judge an argument alike, so the setting cannot stay an SDK default that only
     * one of them relies on.
     */
    @Bean
    McpStatelessSyncServer mcpServer(
            HttpServletStatelessServerTransport mcpTransport,
            McpJsonMapper mcpJsonMapper,
            JsonSchemaValidator mcpSchemaValidator,
            McpCatalog mcpCatalog) {
        return McpServer.sync(mcpTransport)
                .jsonMapper(mcpJsonMapper)
                .jsonSchemaValidator(mcpSchemaValidator)
                .validateToolInputs(McpCatalog.VALIDATE_TOOL_INPUTS)
                .serverInfo(applicationName, applicationVersion)
                .instructions(INSTRUCTIONS)
                .immediateExecution(true)
                .tools(mcpCatalog.tools())
                .resources(mcpCatalog.resources())
                .build();
    }
}
