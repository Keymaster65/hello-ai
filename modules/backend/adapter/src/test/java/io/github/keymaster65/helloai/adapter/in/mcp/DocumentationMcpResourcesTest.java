package io.github.keymaster65.helloai.adapter.in.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.keymaster65.helloai.application.port.in.DocumentationService;
import io.github.keymaster65.helloai.domain.model.DocumentationPage;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The documentation pages as MCP resources: one specification per page, each with the URI a client
 * reads it under.
 */
@ExtendWith(MockitoExtension.class)
class DocumentationMcpResourcesTest {

    @Mock
    private DocumentationService documentationService;

    private DocumentationMcpResources resources() {
        return new DocumentationMcpResources(documentationService);
    }

    @Test
    void shouldOfferOneResourcePerPage() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of(
                new DocumentationPage("system", "recipes – Systemdokumentation"),
                new DocumentationPage("adr/0049-mcp", "ADR 0049")));

        // Act
        List<SyncResourceSpecification> specifications = resources().specifications();

        // Assert
        assertThat(specifications).extracting(specification -> specification.resource().uri())
                .containsExactly("recipes://docs/system", "recipes://docs/adr/0049-mcp");
        assertThat(specifications).allSatisfy(specification -> {
            assertThat(specification.resource().mimeType()).isEqualTo("text/html");
            assertThat(specification.resource().title()).isNotBlank();
        });
    }

    @Test
    void shouldOfferNothing_whenNoDocumentationWasPacked() {
        // Arrange – built with -PskipDocs.
        when(documentationService.getAll()).thenReturn(List.of());

        // Act & Assert
        assertThat(resources().specifications()).isEmpty();
    }

    @Test
    void shouldReadThePageBehindTheUri() {
        // Arrange
        when(documentationService.getAll())
                .thenReturn(List.of(new DocumentationPage("system", "recipes – Systemdokumentation")));
        when(documentationService.getContent("system")).thenReturn("<html>…</html>");

        SyncResourceSpecification specification = resources().specifications().getFirst();

        // Act
        McpSchema.ReadResourceResult result = specification.readHandler().apply(
                McpTransportContext.EMPTY,
                McpSchema.ReadResourceRequest.builder("recipes://docs/system").build());

        // Assert
        assertThat(result.contents()).hasSize(1);
        McpSchema.TextResourceContents contents = (McpSchema.TextResourceContents) result.contents().getFirst();
        assertThat(contents.uri()).isEqualTo("recipes://docs/system");
        assertThat(contents.mimeType()).isEqualTo("text/html");
        assertThat(contents.text()).isEqualTo("<html>…</html>");
    }
}
