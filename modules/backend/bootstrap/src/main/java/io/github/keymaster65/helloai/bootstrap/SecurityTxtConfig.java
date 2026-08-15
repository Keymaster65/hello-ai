package io.github.keymaster65.helloai.bootstrap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hangs the {@link SecurityTxtValve} into the Tomcat pipeline, so the reporting channel is
 * reachable where RFC 9116 says it is (see ADR 0037).
 *
 * <p>An <em>engine</em> valve, not a context valve: only there does it see requests that map to
 * no context – and a request to the origin, outside the context path {@code /recipes}, is
 * exactly that.
 */
@Configuration
@EnableConfigurationProperties(SecurityTxtProperties.class)
public class SecurityTxtConfig {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> securityTxtCustomizer(
            SecurityTxtProperties properties) {
        return factory -> factory.addEngineValves(new SecurityTxtValve(properties));
    }
}
