package io.github.keymaster65.helloai.adapter.out.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.keymaster65.helloai.domain.model.DocumentationPage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Reads the documentation from the test classpath, where {@code src/test/resources/static/docs}
 * stands in for what the build packs into the jar: {@code index.html} plus one rendered decision.
 *
 * <p>No Spring context and no mock of the resolver: what is worth testing here is exactly the part
 * a mock would replace – finding the files, reading their titles, and refusing an identifier that
 * is not in the index.
 */
class ClasspathDocumentationRepositoryTest {

    private static final ResourcePatternResolver RESOLVER = new PathMatchingResourcePatternResolver();

    private static final String ADR_ID = "adr/0001-test-entscheidung";

    private ClasspathDocumentationRepository repository() {
        return new ClasspathDocumentationRepository(RESOLVER);
    }

    @Test
    void shouldIndexTheChaptersAndEveryDecision() {
        // Act
        List<DocumentationPage> pages = repository().findAll();

        // Assert – the chapters first, the decisions in the order of their numbers.
        assertThat(pages).extracting(DocumentationPage::id).containsExactly("system", ADR_ID);
    }

    @Test
    void shouldTakeTheTitleFromTheHtmlHead() {
        // Act
        List<DocumentationPage> pages = repository().findAll();

        // Assert
        assertThat(pages).extracting(DocumentationPage::title)
                .containsExactly("recipes – Systemdokumentation", "ADR 0001: Test-Entscheidung");
    }

    @Test
    void shouldReadTheContentOfAPage() {
        // Act & Assert
        assertThat(repository().findContent("system"))
                .hasValueSatisfying(content -> assertThat(content).contains("Stellvertreter der gerenderten Kapitel"));
        assertThat(repository().findContent(ADR_ID))
                .hasValueSatisfying(content -> assertThat(content).contains("ADR 0001"));
    }

    @Test
    void shouldReturnNothing_whenTheIdentifierIsUnknown() {
        // Act & Assert
        assertThat(repository().findContent("gibt-es-nicht")).isEmpty();
    }

    @Test
    void shouldReturnNothing_whenTheIdentifierTriesToLeaveTheIndex() {
        // Act & Assert – identifiers are looked up, never turned into a path.
        assertThat(repository().findContent("../../application.yml")).isEmpty();
        assertThat(repository().findContent("adr/../index")).isEmpty();
    }
}
