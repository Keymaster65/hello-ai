package io.github.keymaster65.helloai.adapter.in.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.keymaster65.helloai.application.port.in.DocumentationService;
import io.github.keymaster65.helloai.application.service.DocumentationNotFoundException;
import io.github.keymaster65.helloai.domain.model.DocumentationPage;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The documentation tools of the MCP adapter. They exist next to the resources of
 * {@link DocumentationMcpResources} for clients that support tools only, and both read through the
 * same port – which is mocked here.
 */
@ExtendWith(MockitoExtension.class)
class DocumentationMcpToolsTest {

    private static final McpJsonMapper JSON = McpJsonDefaults.getMapper();

    @Mock
    private DocumentationService documentationService;

    private DocumentationMcpTools tools() {
        return new DocumentationMcpTools(documentationService, JSON);
    }

    private McpSchema.CallToolResult call(String name, Map<String, Object> arguments) {
        SyncToolSpecification specification = tools().specifications().stream()
                .filter(candidate -> candidate.tool().name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No tool named " + name));
        return specification.callHandler()
                .apply(McpTransportContext.EMPTY, new McpSchema.CallToolRequest(name, arguments, null));
    }

    private static String text(McpSchema.CallToolResult result) {
        assertThat(result.content()).hasSize(1);
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }

    @Test
    void shouldOfferTwoReadOnlyTools() {
        // Act
        List<SyncToolSpecification> specifications = tools().specifications();

        // Assert
        assertThat(specifications).extracting(specification -> specification.tool().name())
                .containsExactly("list_documentation", "read_documentation");
        assertThat(specifications).allSatisfy(specification ->
                assertThat(specification.tool().annotations().readOnlyHint()).isTrue());
    }

    @Test
    void shouldListPagesWithTheirResourceUri() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of(
                new DocumentationPage("system", "recipes – Systemdokumentation"),
                new DocumentationPage("adr/0049-mcp", "ADR 0049")));

        // Act
        String json = text(call("list_documentation", Map.of()));

        // Assert – the URI is the bridge to the same page as a resource.
        assertThat(json).contains("\"id\":\"system\"", "\"uri\":\"recipes://docs/system\"");
        assertThat(json).contains("\"uri\":\"recipes://docs/adr/0049-mcp\"");
    }

    @Test
    void shouldReturnThePageAsDeliveredHtml() {
        // Arrange
        when(documentationService.getContent("system")).thenReturn("<html><title>x</title></html>");

        // Act
        McpSchema.CallToolResult result = call("read_documentation", Map.of("id", "system"));

        // Assert – handed on unchanged; the page is delivered as HTML (ADR 0024).
        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(text(result)).isEqualTo("<html><title>x</title></html>");
    }

    @Test
    void shouldReportAnError_whenThePageDoesNotExist() {
        // Arrange
        when(documentationService.getContent("gibt-es-nicht"))
                .thenThrow(new DocumentationNotFoundException("gibt-es-nicht"));

        // Act
        McpSchema.CallToolResult result = call("read_documentation", Map.of("id", "gibt-es-nicht"));

        // Assert
        assertThat(result.isError()).isTrue();
        assertThat(text(result)).isEqualTo("No documentation page exists with the identifier gibt-es-nicht");
    }

    @Test
    void shouldReportAnError_whenTheIdentifierIsBlank() {
        // Act
        McpSchema.CallToolResult result = call("read_documentation", Map.of("id", "  "));

        // Assert
        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("id");
    }
}
