package io.github.keymaster65.helloai.application.service;

import io.github.keymaster65.helloai.application.port.in.DocumentationService;
import io.github.keymaster65.helloai.application.port.out.DocumentationRepository;
import io.github.keymaster65.helloai.domain.model.DocumentationPage;
import java.util.List;

/**
 * Application service implementing the documentation use cases (ADR 0049). Turns the absent page
 * of the outbound port into the exception the inbound port promises &ndash; the same shape as
 * {@link RecipeServiceImpl}.
 *
 * <p>Plain Java on purpose: this layer carries no framework annotations at all (ADR 0045).
 */
public class DocumentationServiceImpl implements DocumentationService {

    private final DocumentationRepository documentationRepository;

    public DocumentationServiceImpl(DocumentationRepository documentationRepository) {
        this.documentationRepository = documentationRepository;
    }

    @Override
    public List<DocumentationPage> getAll() {
        return documentationRepository.findAll();
    }

    @Override
    public String getContent(String id) {
        return documentationRepository.findContent(id)
                .orElseThrow(() -> new DocumentationNotFoundException(id));
    }
}
