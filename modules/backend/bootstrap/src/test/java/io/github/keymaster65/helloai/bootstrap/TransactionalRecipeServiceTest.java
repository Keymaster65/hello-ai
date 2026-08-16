package io.github.keymaster65.helloai.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.keymaster65.helloai.application.port.in.RecipeService;
import io.github.keymaster65.helloai.domain.model.Difficulty;
import io.github.keymaster65.helloai.domain.model.Recipe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * The decorator carries the transaction boundary that {@code @Transactional} used to carry inside
 * the application layer (docs/prompt/architektur.adoc). Two things are worth a test: every use case is delegated, and
 * reads ask for a read-only transaction while writes do not.
 */
@ExtendWith(MockitoExtension.class)
class TransactionalRecipeServiceTest {

    @Mock
    private RecipeService delegate;

    @Mock
    private PlatformTransactionManager transactionManager;

    private static final Recipe RECIPE =
            new Recipe(1L, "Pancakes", "Fluffy", 2, 15, Difficulty.EASY, List.of(), List.of());

    private TransactionalRecipeService serviceWithStartedTransaction() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return new TransactionalRecipeService(delegate, transactionManager);
    }

    private TransactionDefinition capturedDefinition() {
        ArgumentCaptor<TransactionDefinition> definition =
                ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(definition.capture());
        return definition.getValue();
    }

    @Test
    void shouldReadInAReadOnlyTransaction_whenGettingById() {
        // Arrange
        TransactionalRecipeService service = serviceWithStartedTransaction();
        when(delegate.getById(1L)).thenReturn(RECIPE);

        // Act
        Recipe result = service.getById(1L);

        // Assert
        assertThat(result).isEqualTo(RECIPE);
        assertThat(capturedDefinition().isReadOnly()).isTrue();
    }

    @Test
    void shouldReadInAReadOnlyTransaction_whenGettingAll() {
        // Arrange
        TransactionalRecipeService service = serviceWithStartedTransaction();
        when(delegate.getAll()).thenReturn(List.of(RECIPE));

        // Act
        List<Recipe> result = service.getAll();

        // Assert
        assertThat(result).containsExactly(RECIPE);
        assertThat(capturedDefinition().isReadOnly()).isTrue();
    }

    @Test
    void shouldWriteInAReadWriteTransaction_whenCreating() {
        // Arrange
        TransactionalRecipeService service = serviceWithStartedTransaction();
        when(delegate.create(RECIPE)).thenReturn(RECIPE);

        // Act
        Recipe result = service.create(RECIPE);

        // Assert
        assertThat(result).isEqualTo(RECIPE);
        assertThat(capturedDefinition().isReadOnly()).isFalse();
    }

    @Test
    void shouldWriteInAReadWriteTransaction_whenUpdating() {
        // Arrange
        TransactionalRecipeService service = serviceWithStartedTransaction();
        when(delegate.update(1L, RECIPE)).thenReturn(RECIPE);

        // Act
        Recipe result = service.update(1L, RECIPE);

        // Assert
        assertThat(result).isEqualTo(RECIPE);
        assertThat(capturedDefinition().isReadOnly()).isFalse();
    }

    @Test
    void shouldWriteInAReadWriteTransaction_whenDeleting() {
        // Arrange
        TransactionalRecipeService service = serviceWithStartedTransaction();

        // Act
        service.delete(1L);

        // Assert
        verify(delegate).delete(1L);
        assertThat(capturedDefinition().isReadOnly()).isFalse();
    }

    @Test
    void shouldRollBack_whenAUseCaseThrows() {
        // Arrange
        TransactionStatus status = new SimpleTransactionStatus();
        when(transactionManager.getTransaction(any())).thenReturn(status);
        TransactionalRecipeService service =
                new TransactionalRecipeService(delegate, transactionManager);
        doThrow(new IllegalStateException("boom")).when(delegate).delete(99L);

        // Act & Assert
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(IllegalStateException.class);
        verify(transactionManager).rollback(status);
    }
}
