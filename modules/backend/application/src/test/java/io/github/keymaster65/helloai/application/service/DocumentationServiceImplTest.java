package io.github.keymaster65.helloai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.keymaster65.helloai.application.port.out.DocumentationRepository;
import io.github.keymaster65.helloai.domain.model.DocumentationPage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentationServiceImplTest {

    @Mock
    private DocumentationRepository documentationRepository;

    @InjectMocks
    private DocumentationServiceImpl documentationService;

    @Test
    void shouldReturnPages_whenDocumentationIsPresent() {
        // Arrange
        DocumentationPage page = new DocumentationPage("system", "recipes – Systemdokumentation");
        when(documentationRepository.findAll()).thenReturn(List.of(page));

        // Act
        List<DocumentationPage> result = documentationService.getAll();

        // Assert
        assertThat(result).containsExactly(page);
        verify(documentationRepository).findAll();
    }

    @Test
    void shouldReturnEmptyList_whenNoDocumentationWasPacked() {
        // Arrange – a deployable built with -PskipDocs carries no documentation.
        when(documentationRepository.findAll()).thenReturn(List.of());

        // Act & Assert
        assertThat(documentationService.getAll()).isEmpty();
    }

    @Test
    void shouldReturnContent_whenPageExists() {
        // Arrange
        when(documentationRepository.findContent("system")).thenReturn(Optional.of("<html>…</html>"));

        // Act
        String result = documentationService.getContent("system");

        // Assert
        assertThat(result).isEqualTo("<html>…</html>");
    }

    @Test
    void shouldThrowException_whenPageNotFound() {
        // Arrange
        when(documentationRepository.findContent("gibt-es-nicht")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> documentationService.getContent("gibt-es-nicht"))
                .isInstanceOf(DocumentationNotFoundException.class)
                .hasMessageContaining("gibt-es-nicht");
    }

    @Test
    void shouldCarryTheIdentifier_whenThrowing() {
        // Arrange – the identifier is what the adapter puts into its error message.
        when(documentationRepository.findContent("adr/0049")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> documentationService.getContent("adr/0049"))
                .isInstanceOfSatisfying(DocumentationNotFoundException.class,
                        exception -> assertThat(exception.id()).isEqualTo("adr/0049"));
    }
}
