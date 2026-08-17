package io.github.keymaster65.helloai.adapter.in.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.keymaster65.helloai.application.port.in.DocumentationService;
import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.github.keymaster65.helloai.domain.model.Difficulty;
import io.github.keymaster65.helloai.domain.model.DocumentationPage;
import io.github.keymaster65.helloai.domain.model.Recipe;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The catalogue both fronts of this adapter run on (ADR 0050): the tools and resources of the
 * server, and the two calls that execute them.
 *
 * <p>The point under test is that the catalogue behaves like the protocol endpoint does &ndash;
 * same tools, same schema check before a handler runs, an unknown name as a miss rather than an
 * exception. The use cases behind it are mocked; this is the adapter under test.
 */
@ExtendWith(MockitoExtension.class)
class McpCatalogTest {

    private static final McpJsonMapper JSON = McpJsonDefaults.getMapper();
    private static final JsonSchemaValidator VALIDATOR = McpJsonDefaults.getSchemaValidator();

    @Mock
    private RecipeService recipeService;

    @Mock
    private DocumentationService documentationService;

    private McpCatalog catalog() {
        return new McpCatalog(
                new RecipeMcpTools(recipeService, JSON),
                new DocumentationMcpTools(documentationService, JSON),
                new DocumentationMcpResources(documentationService),
                VALIDATOR);
    }

    private static Recipe pancakes() {
        return new Recipe(1L, "Pancakes", "Fluffy", 2, 15, Difficulty.EASY, List.of(), List.of());
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }

    @Test
    void shouldHoldTheToolsOfBothSources() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of());

        // Act
        List<String> names = catalog().tools().stream().map(specification -> specification.tool().name()).toList();

        // Assert – the same four tools the protocol endpoint offers, in the same order.
        assertThat(names).containsExactly("list_recipes", "get_recipe", "list_documentation", "read_documentation");
    }

    @Test
    void shouldHoldOneResourcePerDocumentationPage() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of(new DocumentationPage("system", "Systemdokumentation")));

        // Act
        McpCatalog catalog = catalog();

        // Assert
        assertThat(catalog.resources()).hasSize(1);
        assertThat(catalog.resources().getFirst().resource().uri()).isEqualTo("recipes://docs/system");
    }

    @Test
    void shouldBuildTheCatalogueOnce() {
        // Arrange – the resource catalogue of a stateless server is fixed at start-up (ADR 0049).
        when(documentationService.getAll()).thenReturn(List.of(new DocumentationPage("system", "Systemdokumentation")));
        McpCatalog catalog = catalog();

        // Act
        List<?> first = catalog.resources();
        List<?> second = catalog.resources();

        // Assert
        assertThat(first).isSameAs(second);
    }

    @Test
    void shouldRunAToolAndReturnItsResult() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of());
        when(recipeService.getById(1L)).thenReturn(pancakes());

        // Act
        Optional<McpSchema.CallToolResult> result = catalog().callTool("get_recipe", Map.of("id", 1));

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(text(result.get())).contains("\"title\":\"Pancakes\"");
    }

    @Test
    void shouldTreatAToolWithoutArgumentsAsAnEmptyObject() {
        // Arrange – a caller may omit the body entirely; the schema still has to be satisfied.
        when(documentationService.getAll()).thenReturn(List.of());
        when(recipeService.getAll()).thenReturn(List.of(pancakes()));

        // Act
        Optional<McpSchema.CallToolResult> result = catalog().callTool("list_recipes", null);

        // Assert
        assertThat(result).isPresent();
        assertThat(text(result.get())).startsWith("[");
    }

    @Test
    void shouldRejectArgumentsAgainstTheSchemaBeforeTheHandlerRuns() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of());

        // Act – the schema demands a whole number, so the use case must never be reached.
        Optional<McpSchema.CallToolResult> result = catalog().callTool("get_recipe", Map.of("id", "eins"));

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().isError()).isTrue();
        verifyNoInteractions(recipeService);
    }

    @Test
    void shouldReportAMissForAnUnknownTool() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of());

        // Act & Assert – an unknown name is not an error of the call, it is no call at all.
        assertThat(catalog().callTool("drop_recipes", Map.of())).isEmpty();
    }

    @Test
    void shouldReadAResource() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of(new DocumentationPage("system", "Systemdokumentation")));
        when(documentationService.getContent("system")).thenReturn("<html><title>Systemdokumentation</title></html>");

        // Act
        Optional<McpSchema.ReadResourceResult> result = catalog().readResource("recipes://docs/system");

        // Assert
        assertThat(result).isPresent();
        McpSchema.TextResourceContents contents = (McpSchema.TextResourceContents) result.get().contents().getFirst();
        assertThat(contents.mimeType()).isEqualTo("text/html");
        assertThat(contents.text()).contains("Systemdokumentation");
    }

    @Test
    void shouldReportAMissForAnUnknownResource() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of());

        // Act & Assert
        assertThat(catalog().readResource("recipes://docs/nirgendwo")).isEmpty();
    }
}
