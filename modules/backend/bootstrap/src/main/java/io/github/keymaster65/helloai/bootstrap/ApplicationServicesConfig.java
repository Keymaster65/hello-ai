package io.github.keymaster65.helloai.bootstrap;

import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.github.keymaster65.helloai.application.port.out.RecipeRepository;
import io.github.keymaster65.helloai.application.service.RecipeServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Registers the application services as beans. Since the application layer carries no framework
 * annotations (ADR 0045), component scanning cannot find them &ndash; they are wired here, in the
 * composition root, where knowing about Spring is the job.
 */
@Configuration
class ApplicationServicesConfig {

    /**
     * The only {@link RecipeService} bean: the plain use cases, wrapped in the transaction
     * boundary. Callers see the port, not which of the two objects they are talking to.
     */
    @Bean
    RecipeService recipeService(
            RecipeRepository recipeRepository, PlatformTransactionManager transactionManager) {
        return new TransactionalRecipeService(
                new RecipeServiceImpl(recipeRepository), transactionManager);
    }
}
