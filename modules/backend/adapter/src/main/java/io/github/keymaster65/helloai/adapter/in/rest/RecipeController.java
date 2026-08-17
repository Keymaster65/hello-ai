package io.github.keymaster65.helloai.adapter.in.rest;

import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for managing recipes, backed by the relational store &ndash; the way of the
 * application. The operations themselves are in {@link RecipeControllerBase}; this class says
 * <em>where</em> they answer and which use-case port serves them.
 *
 * <p>Injected is the primary {@link RecipeService} bean, the one wrapped in the transaction
 * boundary of the composition root. Since ADR 0054 it is not the only one &ndash; the second front
 * is {@link GitDataRecipeController}.
 */
@RestController
@RequestMapping(RecipeController.PATH)
@Tag(name = "Recipes", description = "Create, read, update and delete recipes")
public class RecipeController extends RecipeControllerBase {

    /** Address of this front; also the prefix of the {@code Location} header. */
    public static final String PATH = "/api/recipes";

    public RecipeController(RecipeService recipeService, RecipeRestMapper mapper) {
        super(recipeService, mapper);
    }

    @Override
    protected String basePath() {
        return PATH;
    }
}
