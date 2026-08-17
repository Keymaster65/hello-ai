package io.github.keymaster65.helloai.adapter.out.gitdata;

import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.github.keymaster65.helloai.application.port.out.RecipeRepository;
import io.github.keymaster65.helloai.application.service.RecipeServiceImpl;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * Assembles the git-backed store and the use case that sits on it (ADR 0054).
 *
 * <p>It sits <em>in the adapter</em> for the same reason {@code McpServerConfig} does: the onion
 * rule says adapters do not know each other (ADR 0019), and {@code :bootstrap} is itself the
 * outermost adapter ring &ndash; it must not reach for the classes here. What leaves this package
 * are two beans, and both are typed as <em>ports</em>: {@link RecipeRepository} and
 * {@link RecipeService}. Nobody outside learns that git is involved.
 *
 * <p>Only present when {@code recipes.gitdata.enabled} is {@code true}. Without it there is no
 * repository handle, no second use case and &ndash; by the same condition on the controller &ndash;
 * no address. The relational path stays exactly as it was; it keeps {@code @Primary}.
 */
@Configuration
@EnableConfigurationProperties(GitDataProperties.class)
@ConditionalOnProperty(prefix = "recipes.gitdata", name = "enabled", havingValue = "true")
class GitDataConfig {

    /**
     * Name of the store bean. The use case below asks for it by name, because by type there are two
     * of them and the other one is the primary.
     */
    static final String REPOSITORY_BEAN = "gitDataRecipeRepository";

    /**
     * Name of the use-case bean. {@code GitDataRecipeController} asks for exactly this name &ndash;
     * as a literal, because the inbound adapter must not import anything from this package either.
     */
    static final String SERVICE_BEAN = "gitDataRecipeService";

    /**
     * The store: the second implementation of the outbound port. It opens (or creates) its bare
     * repository and is closed with the context, because a repository holds file handles.
     *
     * @param properties directory and branch
     * @param json       the Jackson mapper Spring Boot configures
     * @return the git-backed store, as the port
     */
    @Bean(name = REPOSITORY_BEAN, destroyMethod = "close")
    GitDataRecipeRepository gitDataRecipeRepository(GitDataProperties properties, ObjectMapper json) {
        return GitDataRecipeRepository.openBare(
                Path.of(properties.repository()), properties.branch(), Clock.systemUTC(), json);
    }

    /**
     * The initial population: the recipes of the seed changeset, written when the branch does not
     * exist yet (ADR 0055). An existing branch is left alone, whatever stands on it.
     *
     * @param store the git-backed store
     * @return the population, which runs before the first request
     */
    @Bean
    GitDataInitialPopulation gitDataInitialPopulation(GitDataRecipeRepository store) {
        return new GitDataInitialPopulation(store);
    }

    /**
     * The second {@link RecipeService}, on the git-backed store.
     *
     * <p>Deliberately <em>without</em> the transaction boundary that wraps the relational use case
     * in the composition root: a {@code TransactionTemplate} around a git commit would open a
     * database transaction that has nothing to do with the write and promise an atomicity it does
     * not provide. Here the commit is the transaction (ADR 0053).
     *
     * @param repository the git-backed store
     * @return the use cases on that store
     */
    @Bean(SERVICE_BEAN)
    RecipeService gitDataRecipeService(@Qualifier(REPOSITORY_BEAN) RecipeRepository repository) {
        return new RecipeServiceImpl(repository);
    }
}
