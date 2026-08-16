package io.github.keymaster65.helloai.adapter.out.documentation;

import io.github.keymaster65.helloai.application.port.out.DocumentationRepository;
import io.github.keymaster65.helloai.domain.model.DocumentationPage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Repository;

/**
 * Reads the rendered system documentation from the classpath, where the build puts it: the
 * chapters as {@code static/docs/index.html}, the architecture decisions as
 * {@code static/docs/adr/*.html} (ADR 0024). This is the outbound adapter behind the
 * {@link DocumentationRepository} port (ADR 0049).
 *
 * <p>The index is built <em>once</em>, in the constructor. The documentation travels inside the
 * deployable and cannot change while the application runs, so a second scan would read the same
 * bytes; a failure to read it shows up at start-up instead of on the first request.
 *
 * <p>The index maps an identifier to the resource it was found at. Content is therefore never
 * looked up by building a path from a caller's string &ndash; an identifier that is not in the
 * map is simply absent, which is what keeps {@code ../} out of the picture.
 *
 * <p>With {@code -PskipDocs} no documentation is packed into the jar. The index is then empty,
 * and both port methods answer accordingly instead of failing.
 */
@Repository
class ClasspathDocumentationRepository implements DocumentationRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathDocumentationRepository.class);

    /** The rendered chapter work; {@code system.adoc} is packed under this name (ADR 0024). */
    private static final String CHAPTERS_LOCATION = "classpath*:static/docs/index.html";

    /** Identifier of the chapter work &ndash; the file name {@code index} would say nothing. */
    private static final String CHAPTERS_ID = "system";

    /** One rendered document per architecture decision, next to the chapters (ADR 0026). */
    private static final String ADR_LOCATION = "classpath*:static/docs/adr/*.html";

    /** Identifiers of the decisions keep the directory, so they read like their link target. */
    private static final String ADR_ID_PREFIX = "adr/";

    /** The title Asciidoctor writes into the HTML head; the fallback is the identifier. */
    private static final Pattern TITLE = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL);

    /**
     * Enough of a document to hold its {@code <head>}. The chapter work embeds its stylesheet
     * after the title, so reading it whole would mean megabytes for one line of interest.
     */
    private static final int TITLE_SEARCH_LIMIT = 64 * 1024;

    private final List<DocumentationPage> pages;
    private final Map<String, Resource> resourcesById;

    ClasspathDocumentationRepository(ResourcePatternResolver resourcePatternResolver) {
        Map<String, Resource> found = new LinkedHashMap<>();
        chapters(resourcePatternResolver).ifPresent(resource -> found.put(CHAPTERS_ID, resource));
        found.putAll(decisions(resourcePatternResolver));

        this.resourcesById = Map.copyOf(found);
        this.pages = found.entrySet().stream()
                .map(entry -> new DocumentationPage(entry.getKey(), title(entry.getValue(), entry.getKey())))
                .toList();

        LOGGER.info("Documentation index built: {} pages", pages.size());
    }

    @Override
    public List<DocumentationPage> findAll() {
        return pages;
    }

    @Override
    public Optional<String> findContent(String id) {
        return Optional.ofNullable(resourcesById.get(id)).map(ClasspathDocumentationRepository::read);
    }

    private static Optional<Resource> chapters(ResourcePatternResolver resolver) {
        return resolve(resolver, CHAPTERS_LOCATION).findFirst();
    }

    /** The decisions in the order of their number, which is the order of their file names. */
    private static Map<String, Resource> decisions(ResourcePatternResolver resolver) {
        List<Resource> resources = new ArrayList<>(resolve(resolver, ADR_LOCATION).toList());
        resources.sort(Comparator.comparing(resource -> String.valueOf(resource.getFilename())));

        Map<String, Resource> byId = new LinkedHashMap<>();
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null) {
                continue;
            }
            byId.put(ADR_ID_PREFIX + filename.substring(0, filename.lastIndexOf('.')), resource);
        }
        return byId;
    }

    /**
     * A missing location is not an error here: the deployable may have been built without the
     * documentation, and the empty index is the honest answer.
     */
    private static Stream<Resource> resolve(ResourcePatternResolver resolver, String location) {
        try {
            return Stream.of(resolver.getResources(location)).filter(Resource::isReadable);
        } catch (IOException e) {
            LOGGER.warn("No documentation found at {}: {}", location, e.getMessage());
            return Stream.empty();
        }
    }

    private static String title(Resource resource, String fallback) {
        try (BufferedReader reader = reader(resource)) {
            char[] head = new char[TITLE_SEARCH_LIMIT];
            // Read until the buffer is full or the file ends: a single read may return less than
            // it could, and the title would then be cut off by an accident of buffering.
            int filled = 0;
            while (filled < head.length) {
                int read = reader.read(head, filled, head.length - filled);
                if (read < 0) {
                    break;
                }
                filled += read;
            }
            if (filled == 0) {
                return fallback;
            }
            Matcher matcher = TITLE.matcher(new String(head, 0, filled));
            if (!matcher.find()) {
                return fallback;
            }
            String title = matcher.group(1).strip();
            return title.isBlank() ? fallback : title;
        } catch (IOException e) {
            LOGGER.warn("Cannot read the title of {}: {}", fallback, e.getMessage());
            return fallback;
        }
    }

    private static String read(Resource resource) {
        try (BufferedReader reader = reader(resource)) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read documentation page " + resource.getFilename(), e);
        }
    }

    private static BufferedReader reader(Resource resource) throws IOException {
        return new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
    }
}
