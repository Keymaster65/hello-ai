package io.github.keymaster65.helloai.adapter.in.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.github.keymaster65.helloai.adapter.in.mcp.dto.McpResourceContentDto;
import io.github.keymaster65.helloai.adapter.in.mcp.dto.McpResourceDto;
import io.github.keymaster65.helloai.adapter.in.mcp.dto.McpToolDto;
import io.github.keymaster65.helloai.adapter.in.mcp.dto.McpToolResultDto;
import io.github.keymaster65.helloai.application.port.in.DocumentationService;
import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.github.keymaster65.helloai.application.service.RecipeNotFoundException;
import io.github.keymaster65.helloai.domain.model.Difficulty;
import io.github.keymaster65.helloai.domain.model.DocumentationPage;
import io.github.keymaster65.helloai.domain.model.Recipe;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * The REST facade of the MCP adapter (ADR 0050): what a caller of the OpenAPI contract sees.
 *
 * <p>Tested without a Spring context &ndash; the controller is a plain object over
 * {@link McpCatalog}, and the two things worth checking are the shape of the DTOs and the two
 * answers the catalogue cannot give itself: {@code 404} for a name nobody carries.
 */
@ExtendWith(MockitoExtension.class)
class McpRestControllerTest {

    private static final McpJsonMapper JSON = McpJsonDefaults.getMapper();
    private static final JsonSchemaValidator VALIDATOR = McpJsonDefaults.getSchemaValidator();

    @Mock
    private RecipeService recipeService;

    @Mock
    private DocumentationService documentationService;

    @Mock
    private McpStatelessSyncServer server;

    private McpRestController controller() {
        McpCatalog catalog = new McpCatalog(
                new RecipeMcpTools(recipeService, JSON),
                new DocumentationMcpTools(documentationService, JSON),
                new DocumentationMcpResources(documentationService),
                VALIDATOR);
        return new McpRestController(catalog, server);
    }

    private static Recipe pancakes() {
        return new Recipe(1L, "Pancakes", "Fluffy", 2, 15, Difficulty.EASY, List.of(), List.of());
    }

    private static DocumentationPage chapters() {
        return new DocumentationPage("system", "Systemdokumentation");
    }

    @Test
    void shouldReportNameVersionAndInstructions() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of());
        when(server.getServerInfo()).thenReturn(McpSchema.Implementation.builder("recipes", "1.2.3").build());

        // Act
        var info = controller().server();

        // Assert – the instructions are the same text a protocol client is given.
        assertThat(info.name()).isEqualTo("recipes");
        assertThat(info.version()).isEqualTo("1.2.3");
        assertThat(info.instructions()).contains("list_recipes");
    }

    @Test
    void shouldListTheToolsWithTheirInputSchema() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of());

        // Act
        List<McpToolDto> tools = controller().tools();

        // Assert – the schema is passed through, because it is what tells a caller what to send.
        assertThat(tools).extracting(McpToolDto::name)
                .containsExactly("list_recipes", "get_recipe", "list_documentation", "read_documentation");
        assertThat(tools).allSatisfy(tool -> assertThat(tool.inputSchema()).containsKey("type"));
        assertThat(tools.get(1).inputSchema()).containsEntry("required", List.of("id"));
    }

    @Test
    void shouldCallAToolAndReturnItsText() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of());
        when(recipeService.getById(1L)).thenReturn(pancakes());

        // Act
        McpToolResultDto result = controller().callTool("get_recipe", Map.of("id", 1));

        // Assert
        assertThat(result.isError()).isFalse();
        assertThat(result.content()).singleElement().asString().contains("\"title\":\"Pancakes\"");
    }

    @Test
    void shouldCallAToolWithoutABody() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of());
        when(recipeService.getAll()).thenReturn(List.of(pancakes()));

        // Act – Swagger UI sends no body for a tool without arguments.
        McpToolResultDto result = controller().callTool("list_recipes", null);

        // Assert
        assertThat(result.isError()).isFalse();
        assertThat(result.content()).singleElement().asString().startsWith("[");
    }

    @Test
    void shouldAnswerAToolErrorWithTheResultItself() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of());
        when(recipeService.getById(99L)).thenThrow(new RecipeNotFoundException(99L));

        // Act – the tool ran and reported a failure; that is a result, not a transport error.
        McpToolResultDto result = controller().callTool("get_recipe", Map.of("id", 99));

        // Assert
        assertThat(result.isError()).isTrue();
        assertThat(result.content()).singleElement().asString()
                .isEqualTo("No recipe exists with the identifier 99")
                .doesNotContain("Exception", "io.github");
    }

    @Test
    void shouldAnswerNotFoundForAnUnknownTool() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of());

        // Act & Assert – a name nobody carries is a missing address, not a failed call.
        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> controller().callTool("drop_recipes", Map.of()))
                .satisfies(exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("No tool exists with the name drop_recipes");
                });
    }

    @Test
    void shouldListTheDocumentationAsResources() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of(chapters()));

        // Act
        List<McpResourceDto> resources = controller().resources();

        // Assert
        assertThat(resources).singleElement().satisfies(resource -> {
            assertThat(resource.uri()).isEqualTo("recipes://docs/system");
            assertThat(resource.name()).isEqualTo("system");
            assertThat(resource.title()).isEqualTo("Systemdokumentation");
            assertThat(resource.mimeType()).isEqualTo("text/html");
        });
    }

    @Test
    void shouldReadAResource() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of(chapters()));
        when(documentationService.getContent("system")).thenReturn("<html><body>Systemdokumentation</body></html>");

        // Act
        List<McpResourceContentDto> contents = controller().resourceContent("recipes://docs/system");

        // Assert
        assertThat(contents).singleElement().satisfies(content -> {
            assertThat(content.uri()).isEqualTo("recipes://docs/system");
            assertThat(content.mimeType()).isEqualTo("text/html");
            assertThat(content.text()).contains("Systemdokumentation");
        });
    }

    @Test
    void shouldAnswerNotFoundForAnUnknownResource() {
        // Arrange
        when(documentationService.getAll()).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> controller().resourceContent("recipes://docs/nirgendwo"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("recipes://docs/nirgendwo");
    }
}
