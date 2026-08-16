package io.github.keymaster65.helloai.systemtest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
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

    /**
     * Performs a GET against the <em>origin</em>, so without the context path – the address space
     * of {@code /.well-known/security.txt} (see ADR 0037).
     */
    static HttpResponse<String> getFromOrigin(String path) {
        return send(HttpRequest.newBuilder(URI.create(RunningApplication.origin() + path))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build());
    }

    static HttpResponse<String> post(String path, String jsonBody) {
        return post(path, jsonBody, Map.of());
    }

    /**
     * Performs a POST with additional headers. The MCP endpoint needs them: its transport insists
     * on an {@code Accept} header naming both {@code application/json} and
     * {@code text/event-stream}, and it answers a request carrying an {@code Origin} with 403
     * (see ADR 0049).
     */
    static HttpResponse<String> post(String path, String jsonBody, Map<String, String> headers) {
        HttpRequest.Builder builder = request(path).header("Content-Type", "application/json");
        headers.forEach(builder::header);
        return send(builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build());
    }

    /** Performs a GET and parses the body as JSON. */
    static JsonNode getJson(String path) {
        return JSON.readTree(get(path).body());
    }

    /** Parses a body as JSON – the MCP tests read the JSON-RPC response of a POST. */
    static JsonNode parse(String body) {
        return JSON.readTree(body);
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
