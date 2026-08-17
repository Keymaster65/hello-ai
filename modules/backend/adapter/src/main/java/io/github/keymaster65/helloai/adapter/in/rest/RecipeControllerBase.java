package io.github.keymaster65.helloai.adapter.in.rest;

import io.github.keymaster65.helloai.adapter.in.rest.dto.RecipeRequest;
import io.github.keymaster65.helloai.adapter.in.rest.dto.RecipeResponse;
import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The five recipe operations, once. Both REST fronts are the same endpoints on the same use-case
 * port &ndash; they differ in their address and in <em>which</em> {@link RecipeService} bean they
 * were handed (ADR 0054).
 *
 * <p>The subclass carries the mapping: {@code @RestController}, {@code @RequestMapping} and the
 * OpenAPI tag belong to the concrete address, everything below to the operation. A copy of this
 * class per store would be five handler methods and their whole contract description in two places,
 * drifting apart at the first change.
 *
 * <p>Which store answers is <em>not</em> visible here, and that is the point: the controller sees
 * the port, not the adapter behind it.
 */
public abstract class RecipeControllerBase {

    /** Errors answer as RFC 9457 problem details, not as plain JSON (see ADR 0046). */
    private static final String ERROR_MEDIA_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final RecipeService recipeService;
    private final RecipeRestMapper mapper;

    /**
     * @param recipeService the use-case port this front talks to
     * @param mapper        translation between DTO and domain
     */
    protected RecipeControllerBase(RecipeService recipeService, RecipeRestMapper mapper) {
        this.recipeService = recipeService;
        this.mapper = mapper;
    }

    /**
     * The path this front is mapped to, without a trailing slash. Used for the {@code Location}
     * header, which has to point at the address the caller actually used.
     *
     * @return the base path, e.g. {@code /api/recipes}
     */
    protected abstract String basePath();

    @PostMapping
    @Operation(summary = "Create a recipe",
            description = "Creates a new recipe and returns it including the generated identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recipe created; Location header points to it"),
            @ApiResponse(responseCode = "400", description = "Request body invalid or malformed",
                    content = @Content(mediaType = ERROR_MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<RecipeResponse> create(
            @Valid @RequestBody RecipeRequest request, UriComponentsBuilder uriBuilder) {
        RecipeResponse created = mapper.toResponse(recipeService.create(mapper.toDomain(request)));
        URI location = uriBuilder.path(basePath() + "/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List all recipes")
    @ApiResponse(responseCode = "200", description = "All recipes, possibly an empty list")
    public List<RecipeResponse> getAll() {
        return recipeService.getAll().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single recipe by its identifier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The requested recipe"),
            @ApiResponse(responseCode = "404", description = "No recipe exists for the given identifier",
                    content = @Content(mediaType = ERROR_MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public RecipeResponse getById(
            @Parameter(description = "Identifier of the recipe", example = "1") @PathVariable long id) {
        return mapper.toResponse(recipeService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an existing recipe",
            description = "Replaces the recipe including its ingredients and preparation steps.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The updated recipe"),
            @ApiResponse(responseCode = "400", description = "Request body invalid or malformed",
                    content = @Content(mediaType = ERROR_MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "No recipe exists for the given identifier",
                    content = @Content(mediaType = ERROR_MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public RecipeResponse update(
            @Parameter(description = "Identifier of the recipe", example = "1") @PathVariable long id,
            @Valid @RequestBody RecipeRequest request) {
        return mapper.toResponse(recipeService.update(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a recipe")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recipe deleted", content = @Content),
            @ApiResponse(responseCode = "404", description = "No recipe exists for the given identifier",
                    content = @Content(mediaType = ERROR_MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifier of the recipe", example = "1") @PathVariable long id) {
        recipeService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
