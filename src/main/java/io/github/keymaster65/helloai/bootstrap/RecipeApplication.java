package io.github.keymaster65.helloai.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point of the recipe management backend.
 */
@SpringBootApplication(scanBasePackages = "io.github.keymaster65.helloai")
public class RecipeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecipeApplication.class, args);
    }
}
