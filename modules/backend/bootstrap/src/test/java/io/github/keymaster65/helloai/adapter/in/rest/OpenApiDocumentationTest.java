package io.github.keymaster65.helloai.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.keymaster65.helloai.bootstrap.RecipeApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that the OpenAPI contract is generated and served, and that it actually describes the
 * recipe endpoints and the REST facade of the MCP server (ADR 0050). Liquibase is switched off so
 * the test needs no database; the DataSource itself is only connected lazily and therefore never
 * used here.
 */
@SpringBootTest(classes = RecipeApplication.class, properties = "spring.liquibase.enabled=false")
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldServeOpenApiDocumentWithApplicationMetadata() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.1")))
                .andExpect(jsonPath("$.info.title").value("Recipe API"))
                .andExpect(jsonPath("$.info.license.name").value("Apache-2.0"));
    }

    @Test
    void shouldDocumentAllRecipeOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/recipes'].get").exists())
                .andExpect(jsonPath("$.paths['/api/recipes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/recipes/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/recipes/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/recipes/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/recipes/{id}'].get.responses.404").exists());
    }

    @Test
    void shouldDocumentRequestAndErrorSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.RecipeRequest").exists())
                .andExpect(jsonPath("$.components.schemas.RecipeResponse").exists())
                .andExpect(jsonPath("$.components.schemas.Ingredient").exists())
                .andExpect(jsonPath("$.components.schemas.ProblemDetail").exists())
                .andExpect(jsonPath("$.components.schemas.RecipeRequest.required").value(
                        org.hamcrest.Matchers.hasItems("title", "difficulty")));
    }

    @Test
    void shouldDocumentTheMcpOperations() throws Exception {
        // The JSON-RPC endpoint is a servlet of its own and invisible to springdoc (ADR 0049); its
        // REST facade is not, and that is the whole point of ADR 0050.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/mcp/server'].get").exists())
                .andExpect(jsonPath("$.paths['/api/mcp/tools'].get").exists())
                .andExpect(jsonPath("$.paths['/api/mcp/tools/{name}'].post").exists())
                .andExpect(jsonPath("$.paths['/api/mcp/tools/{name}'].post.responses.404").exists())
                .andExpect(jsonPath("$.paths['/api/mcp/resources'].get").exists())
                .andExpect(jsonPath("$.paths['/api/mcp/resources/content'].get").exists());
    }

    @Test
    void shouldDocumentTheMcpSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.McpServerInfo").exists())
                .andExpect(jsonPath("$.components.schemas.McpTool").exists())
                .andExpect(jsonPath("$.components.schemas.McpToolResult").exists())
                .andExpect(jsonPath("$.components.schemas.McpResource").exists())
                .andExpect(jsonPath("$.components.schemas.McpResourceContent").exists());
    }

    @Test
    void shouldServeSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }
}
