package io.github.keymaster65.helloai.systemtest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Minimal HTTP client for the system tests. Redirects are <em>not</em> followed automatically so
 * that tests can assert on them.
 */
final class HttpProbe {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private HttpProbe() {
    }

    static HttpResponse<String> get(String path) {
        return send(request(path).GET().build());
    }

    static HttpResponse<String> post(String path, String jsonBody) {
        return send(request(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build());
    }

    /** Performs a GET and parses the body as JSON. */
    static JsonNode getJson(String path) {
        return JSON.readTree(get(path).body());
    }

    private static HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(RunningApplication.baseUrl() + path))
                .timeout(Duration.ofSeconds(30));
    }

    private static HttpResponse<String> send(HttpRequest request) {
        try {
            return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException("Request failed: " + request.uri(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling " + request.uri(), e);
        }
    }
}
