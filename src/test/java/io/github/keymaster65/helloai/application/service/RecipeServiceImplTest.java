package io.github.keymaster65.helloai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.keymaster65.helloai.application.port.out.RecipeRepository;
import io.github.keymaster65.helloai.domain.Difficulty;
import io.github.keymaster65.helloai.domain.Recipe;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeServiceImplTest {

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private RecipeServiceImpl recipeService;

    private Recipe sampleRecipe(Long id) {
        return new Recipe(id, "Pancakes", "Fluffy", 2, 15, Difficulty.EASY, List.of(), List.of());
    }

    @Test
    void shouldReturnRecipe_whenRecipeExists() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(sampleRecipe(1L)));

        // Act
        Recipe result = recipeService.getById(1L);

        // Assert
        assertThat(result.title()).isEqualTo("Pancakes");
        verify(recipeRepository).findById(1L);
    }

    @Test
    void shouldThrowException_whenRecipeNotFoundOnGet() {
        // Arrange
        when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> recipeService.getById(99L))
                .isInstanceOf(RecipeNotFoundException.class);
    }

    @Test
    void shouldPersistRecipe_whenCreating() {
        // Arrange
        Recipe toCreate = sampleRecipe(null);
        when(recipeRepository.save(toCreate)).thenReturn(sampleRecipe(1L));

        // Act
        Recipe created = recipeService.create(toCreate);

        // Assert
        assertThat(created.id()).isEqualTo(1L);
        verify(recipeRepository).save(toCreate);
    }

    @Test
    void shouldReturnUpdatedRecipe_whenUpdatingExisting() {
        // Arrange
        Recipe update = sampleRecipe(null);
        when(recipeRepository.update(1L, update)).thenReturn(Optional.of(sampleRecipe(1L)));

        // Act
        Recipe result = recipeService.update(1L, update);

        // Assert
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowException_whenUpdatingMissingRecipe() {
        // Arrange
        Recipe update = sampleRecipe(null);
        when(recipeRepository.update(99L, update)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> recipeService.update(99L, update))
                .isInstanceOf(RecipeNotFoundException.class);
    }

    @Test
    void shouldThrowException_whenDeletingMissingRecipe() {
        // Arrange
        when(recipeRepository.deleteById(99L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> recipeService.delete(99L))
                .isInstanceOf(RecipeNotFoundException.class);
    }

    @Test
    void shouldDelete_whenRecipeExists() {
        // Arrange
        when(recipeRepository.deleteById(1L)).thenReturn(true);

        // Act
        recipeService.delete(1L);

        // Assert
        verify(recipeRepository).deleteById(1L);
    }
}
