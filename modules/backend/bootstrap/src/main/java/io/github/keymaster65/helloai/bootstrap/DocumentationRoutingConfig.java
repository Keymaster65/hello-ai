package io.github.keymaster65.helloai.bootstrap;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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
 */
@Configuration
public class DocumentationRoutingConfig implements WebMvcConfigurer {

    /**
     * The ADRs travel with the documentation as Markdown, and nothing in the default mappings
     * knows {@code .md}: without this they are served as {@code application/octet-stream}, and the
     * browser offers a download instead of showing the text. Plain text with an explicit charset
     * is the honest answer – no browser renders Markdown either way, but the file is readable,
     * umlauts included.
     *
     * <p>The mapping belongs to the web server rather than to content negotiation: since Spring
     * Framework 7 the static resource handler no longer takes its media types from there.
     */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> markdownAsPlainText() {
        return factory -> {
            MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
            mappings.add("md", MediaType.TEXT_PLAIN_VALUE + ";charset=" + StandardCharsets.UTF_8);
            factory.setMimeMappings(mappings);
        };
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/docs", "/docs/");
        registry.addViewController("/docs/").setViewName("forward:/docs/index.html");
    }
}
