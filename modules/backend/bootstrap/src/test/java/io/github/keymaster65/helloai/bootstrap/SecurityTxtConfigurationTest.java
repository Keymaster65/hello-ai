package io.github.keymaster65.helloai.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Keeps the <em>shipped</em> {@code security.txt} configuration awake (ADR 0037).
 *
 * <p>{@code Expires} ages silently: the file stays reachable, the build stays green, and the file
 * is invalid all the same. This test is the alarm clock the skill {@code docs/prompt/security.adoc}
 * asks for – it turns a rotten date into a red build instead of a quiet lie.
 *
 * <p>Liquibase is switched off so the test needs no database; the DataSource is connected lazily
 * and therefore never used.
 */
@SpringBootTest(classes = RecipeApplication.class, properties = "spring.liquibase.enabled=false")
class SecurityTxtConfigurationTest {

    @Autowired
    private SecurityTxtProperties properties;

    @Test
    void shouldNameAtLeastOneReportingChannel() {
        assertThat(properties.contact()).isNotEmpty();
    }

    @Test
    void shouldNotBeExpired() {
        assertThat(properties.expires())
                .as("Expires of the shipped security.txt")
                .isAfter(OffsetDateTime.now());
    }

    @Test
    void shouldExpireWithinAYear() {
        // RFC 9116 recommends less than a year ahead: a date further out is a promise nobody
        // checks any more.
        assertThat(properties.expires())
                .as("Expires of the shipped security.txt")
                .isBefore(OffsetDateTime.now().plusYears(1));
    }
}
