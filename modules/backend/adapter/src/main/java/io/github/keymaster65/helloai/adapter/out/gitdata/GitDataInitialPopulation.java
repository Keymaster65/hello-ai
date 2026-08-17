package io.github.keymaster65.helloai.adapter.out.gitdata;

import io.github.keymaster65.helloai.domain.model.Recipe;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Writes the seeded recipes into the store when it has never been written to (ADR 0055).
 *
 * <p>The counterpart of the seed changeset of the relational store, and it holds the same rule: it
 * populates an <em>empty</em> store and never an existing one. The condition is the branch, not the
 * number of rows &ndash; a branch somebody emptied on purpose stays empty, exactly as the changeset
 * leaves a database alone in which recipes already stand.
 *
 * <p>It runs as {@link InitializingBean}, so before the web server accepts the first request: a
 * population that could race with a caller would be one that sometimes does not happen.
 */
class GitDataInitialPopulation implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(GitDataInitialPopulation.class);

    private final GitDataRecipeRepository store;

    /**
     * @param store the store to populate
     */
    GitDataInitialPopulation(GitDataRecipeRepository store) {
        this.store = store;
    }

    @Override
    public void afterPropertiesSet() {
        populateIfUnwritten();
    }

    /**
     * Populates the store if its branch does not exist yet.
     *
     * @return the number of recipes written; {@code 0} if there was nothing to do
     */
    int populateIfUnwritten() {
        if (!store.isUnwritten()) {
            LOG.info("gitdata: the branch already exists, the initial population is skipped");
            return 0;
        }
        List<Recipe> recipes = SeedChangelog.recipes();
        // One commit per recipe, like every other write: the history shows the population as the
        // six steps it is, not as one lump nobody can read (ADR 0053).
        recipes.forEach(store::save);
        // The count, not the content: a log line is not the place for the data (Security skill).
        LOG.info("gitdata: the initial population wrote {} recipes", recipes.size());
        return recipes.size();
    }
}
