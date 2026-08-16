package io.github.keymaster65.helloai.bootstrap;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Makes the rendered system documentation reachable under {@code /docs/} (see docs/prompt/systemdokumentation.adoc).
 *
 * <p>The build packs it into the jar as {@code static/docs/index.html}, so Spring Boot already
 * serves it – but only under its file name. A directory index exists for the application root
 * alone; {@code /docs/} would end in a 404. The two mappings below close that gap:
 *
 * <ul>
 *   <li>{@code /docs/} forwards to the file, so the browser keeps the trailing slash. That
 *       matters: the document embeds its diagrams relatively, and without the slash the
 *       browser would resolve them one level too high.
 *   <li>{@code /docs} redirects to {@code /docs/} rather than forwarding, for the same reason.
 *       The redirect is context-relative, so it keeps the context path {@code /recipes}.
 * </ul>
 *
 * <p>View controllers instead of a {@code @Controller}: there is no handler method to write,
 * and springdoc does not mistake them for API operations.
 *
 * <p>Nothing else is needed: everything the document references is HTML or SVG, and both are
 * media types every web server already knows.
 */
@Configuration
public class DocumentationRoutingConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/docs", "/docs/");
        registry.addViewController("/docs/").setViewName("forward:/docs/index.html");
    }
}
