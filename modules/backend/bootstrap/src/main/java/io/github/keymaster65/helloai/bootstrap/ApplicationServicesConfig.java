package io.github.keymaster65.helloai.bootstrap;

import io.github.keymaster65.helloai.application.port.in.DocumentationService;
import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.github.keymaster65.helloai.application.port.out.DocumentationRepository;
import io.github.keymaster65.helloai.application.port.out.RecipeRepository;
import io.github.keymaster65.helloai.application.service.DocumentationServiceImpl;
import io.github.keymaster65.helloai.application.service.RecipeServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Registers the application services as beans. Since the application layer carries no framework
 * annotations (ADR 0045), component scanning cannot find them &ndash; they are wired here, in the
 * composition root, where knowing about Spring is the job.
 */
@Configuration
class ApplicationServicesConfig {

    /**
     * The {@link RecipeService} of the relational store: the plain use cases, wrapped in the
     * transaction boundary. Callers see the port, not which of the two objects they are talking to.
     *
     * <p>{@code @Primary} since ADR 0054: with the git-backed store switched on there is a second
     * bean of this type, and the way of the application stays this one. The other front asks for
     * its bean by name.
     */
    @Bean
    @Primary
    RecipeService recipeService(
            RecipeRepository recipeRepository, PlatformTransactionManager transactionManager) {
        return new TransactionalRecipeService(
                new RecipeServiceImpl(recipeRepository), transactionManager);
    }

    /**
     * The {@link DocumentationService} bean (ADR 0049). No transaction boundary here: the pages are
     * read from the classpath, not from the database, and a {@code TransactionTemplate} around a
     * classpath read would promise something it does not do.
     */
    @Bean
    DocumentationService documentationService(DocumentationRepository documentationRepository) {
        return new DocumentationServiceImpl(documentationRepository);
    }
}
