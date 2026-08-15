package io.github.keymaster65.helloai.systemtest;

import io.github.keymaster65.helloai.bootstrap.RecipeApplication;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The system under test: a real, running application instance that system tests address over HTTP
 * only. They never touch Spring or the database directly.
 *
 * <p>Two modes:
 * <ul>
 *   <li><b>External</b> – if the system property {@code systemtest.baseUrl} is set (Gradle:
 *       {@code -Psystemtest.baseUrl=http://host:port}), the tests run against that already deployed
 *       instance and nothing is started here.</li>
 *   <li><b>Self-hosted</b> (default) – the application is booted once on a free port, backed by an
 *       embedded PostgreSQL, so that {@code gradle systemtest} needs no external infrastructure
 *       (see ADR 0002 for why embedded PostgreSQL instead of Testcontainers).</li>
 * </ul>
 */
final class RunningApplication {

    private static final String BASE_URL_PROPERTY = "systemtest.baseUrl";

    /**
     * Must match {@code server.servlet.context-path} in application.yml (see ADR 0016). It is
     * appended here once, so the tests keep addressing plain paths such as {@code /api/recipes}.
     */
    static final String CONTEXT_PATH = "/recipes";

    private static final String ORIGIN;
    private static final String BASE_URL;
    private static final boolean AVAILABLE;

    static {
        // The property carries the origin (scheme://host:port); the context path is added here.
        String configured = System.getProperty(BASE_URL_PROPERTY, "").trim();
        ORIGIN = configured.isEmpty() ? startLocally() : trimTrailingSlash(configured);
        BASE_URL = ORIGIN == null ? null : ORIGIN + CONTEXT_PATH;
        AVAILABLE = BASE_URL != null;
    }

    private RunningApplication() {
    }

    /**
     * Whether a system under test could be reached or started. Used by {@code @EnabledIf} so the
     * suite is skipped – instead of failing – where the embedded PostgreSQL binary cannot run.
     */
    static boolean available() {
        return AVAILABLE;
    }

    /** Base URL of the running application, without a trailing slash. */
    static String baseUrl() {
        if (BASE_URL == null) {
            throw new IllegalStateException("No running application available");
        }
        return BASE_URL;
    }

    /**
     * Origin of the running application – scheme, host and port, <em>without</em> the context
     * path. RFC 9116 puts {@code security.txt} there and nowhere else (see ADR 0037).
     */
    static String origin() {
        if (ORIGIN == null) {
            throw new IllegalStateException("No running application available");
        }
        return ORIGIN;
    }

    private static String startLocally() {
        EmbeddedPostgres postgres;
        try {
            postgres = EmbeddedPostgres.builder().start();
        } catch (Throwable _) {
            return null;
        }

        int port = freePort();
        // Passed as command-line arguments on purpose: they outrank application.yml, which pins
        // server.port to 80 and the datasource to a local PostgreSQL.
        ConfigurableApplicationContext context = new SpringApplicationBuilder(RecipeApplication.class)
                .run("--server.port=" + port,
                        "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                        "--spring.datasource.username=postgres",
                        "--spring.datasource.password=postgres");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            context.close();
            try {
                postgres.close();
            } catch (IOException _) {
                // Shutting down anyway; nothing useful left to do.
            }
        }));

        return "http://localhost:" + port;
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not reserve a free port for the system test", e);
        }
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
