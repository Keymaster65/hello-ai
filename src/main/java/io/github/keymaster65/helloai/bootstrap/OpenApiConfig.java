package io.github.keymaster65.helloai.bootstrap;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Document-level metadata of the generated OpenAPI contract.
 *
 * <p>Only the parts that are not derivable from the code live here; everything operation- and
 * schema-specific is annotated directly on the REST adapter, so the contract stays next to the
 * code it describes.
 */
@Configuration
public class OpenApiConfig {

    private final String applicationVersion;

    public OpenApiConfig(@Value("${spring.application.version:0.0.1-SNAPSHOT}") String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    @Bean
    public OpenAPI recipeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Recipe API")
                        .description("""
                                REST API for managing recipes including their ingredients and \
                                ordered preparation steps.""")
                        .version(applicationVersion)
                        .contact(new Contact().name("keymaster65"))
                        .license(new License().name("Apache-2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
