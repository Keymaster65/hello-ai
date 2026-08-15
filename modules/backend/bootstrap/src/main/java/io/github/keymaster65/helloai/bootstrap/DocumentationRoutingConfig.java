package io.github.keymaster65.helloai.bootstrap;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Makes the rendered system documentation reachable under {@code /docs/} (see ADR 0024).
 *
 * <p>The build packs it into the jar as {@code static/docs/index.html}, so Spring Boot already
 * serves it – but only under its file name. A directory index exists for the application root
 * alone; {@code /docs/} would end in a 404. The two mappings below close that gap:
 *
 * <ul>
 *   <li>{@code /docs/} forwards to the file, so the browser keeps the trailing slash. That
 *       matters: the document links its ADRs relatively as {@code adr/…}, and without the
 *       slash the browser would resolve them one level too high.
 *   <li>{@code /docs} redirects to {@code /docs/} rather than forwarding, for the same reason.
 *       The redirect is context-relative, so it keeps the context path {@code /recipes}.
 * </ul>
 *
 * <p>View controllers instead of a {@code @Controller}: there is no handler method to write,
 * and springdoc does not mistake them for API operations.
 *
 * <p>Nothing else is needed. The ADRs used to travel as Markdown and required an explicit
 * {@code MimeMappings} entry so the browser would show them instead of offering a download;
 * since ADR 0026 they are rendered to HTML like the documentation itself, and {@code .html} is
 * a media type every web server already knows.
 */
@Configuration
public class DocumentationRoutingConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/docs", "/docs/");
        registry.addViewController("/docs/").setViewName("forward:/docs/index.html");
    }
}
