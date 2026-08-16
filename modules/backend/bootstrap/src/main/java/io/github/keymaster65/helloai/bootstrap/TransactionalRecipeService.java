package io.github.keymaster65.helloai.bootstrap;

import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.github.keymaster65.helloai.domain.model.Recipe;
import java.util.List;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Wraps every use case of a {@link RecipeService} in a transaction. The decorator exists so that
 * the application layer can stay free of framework types (ADR 0045): the boundary is still the
 * use case, but the annotation that used to express it lives out here.
 *
 * <p>Reads run read-only, writes read-write. A {@code RuntimeException} escaping a write &ndash;
 * {@code RecipeNotFoundException}, for instance &ndash; rolls the transaction back, which is the
 * same behaviour {@code @Transactional} gave before.
 */
final class TransactionalRecipeService implements RecipeService {

    private final RecipeService delegate;
    private final TransactionTemplate write;
    private final TransactionTemplate read;

    TransactionalRecipeService(RecipeService delegate, PlatformTransactionManager transactionManager) {
        this.delegate = delegate;
        this.write = new TransactionTemplate(transactionManager);
        this.read = new TransactionTemplate(transactionManager);
        this.read.setReadOnly(true);
    }

    @Override
    public Recipe create(Recipe recipe) {
        return write.execute(status -> delegate.create(recipe));
    }

    @Override
    public Recipe getById(long id) {
        return read.execute(status -> delegate.getById(id));
    }

    @Override
    public List<Recipe> getAll() {
        return read.execute(status -> delegate.getAll());
    }

    @Override
    public Recipe update(long id, Recipe recipe) {
        return write.execute(status -> delegate.update(id, recipe));
    }

    @Override
    public void delete(long id) {
        write.executeWithoutResult(status -> delegate.delete(id));
    }
}
