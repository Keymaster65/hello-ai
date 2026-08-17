package io.github.keymaster65.helloai.adapter.in.rest;

import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The same recipe operations as {@link RecipeController}, but served by the store that keeps every
 * row as a JSON file on a git branch (ADR 0053, ADR 0054).
 *
 * <p>Two things make this class more than a copy of its sibling:
 *
 * <ul>
 *   <li>It asks for the {@link RecipeService} bean <em>by name</em>. Both fronts speak to the same
 *       port; which store answers is decided in the composition root, not here. This controller
 *       does not know that git exists.</li>
 *   <li>It only exists when {@code recipes.gitdata.enabled} is set. Writing into a repository is
 *       nothing an application should start doing because it was deployed &ndash; the address is
 *       absent until someone asks for it.</li>
 * </ul>
 */
@RestController
@RequestMapping(GitDataRecipeController.PATH)
@ConditionalOnProperty(prefix = "recipes.gitdata", name = "enabled", havingValue = "true")
@Tag(name = "Recipes (gitdata)",
        description = "The same operations on the git-backed store: one JSON file per row, one commit per write")
public class GitDataRecipeController extends RecipeControllerBase {

    /** Address of this front; also the prefix of the {@code Location} header. */
    public static final String PATH = "/api/gitdata/recipes";

    /**
     * Name of the use-case bean that sits on the git-backed store. A <em>literal</em>, not a
     * constant of that adapter: adapters do not know each other (ADR 0019), so the bean name is the
     * only thing the two share &ndash; the same shape of coupling as a URL between two systems.
     */
    public static final String SERVICE_BEAN = "gitDataRecipeService";

    public GitDataRecipeController(
            @Qualifier(SERVICE_BEAN) RecipeService recipeService, RecipeRestMapper mapper) {
        super(recipeService, mapper);
    }

    @Override
    protected String basePath() {
        return PATH;
    }
}
