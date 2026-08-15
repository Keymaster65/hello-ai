package io.github.keymaster65.helloai.bootstrap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

/**
 * Serves {@code /.well-known/security.txt} at the <em>origin</em> of the application (RFC 9116,
 * see ADR 0037).
 *
 * <p>The application itself lives under the context path {@code /recipes} (ADR 0016), and RFC
 * 9116 names exactly one place for this file: the origin, not a path below it. Nothing in Spring
 * MVC can answer there – Tomcat maps such a request to no context at all, so there is no
 * dispatcher servlet and no resource handler behind it.
 *
 * <p>A valve can. Tomcat runs the container pipeline even when a request maps to no context; the
 * missing context only becomes a 404 further down, in {@code StandardHostValve}. This valve sits
 * before that point, answers the one path it owns and passes everything else on unchanged – so
 * the context path plays no part here, and a change to it cannot move the file.
 *
 * <p>{@code GET} and {@code HEAD} are answered; any other method falls through and ends as the
 * 404 every other path outside the context path gets.
 */
public final class SecurityTxtValve extends ValveBase {

    /** The one place RFC 9116 allows. Deliberately not configurable – see ADR 0037. */
    static final String PATH = "/.well-known/security.txt";

    private final byte[] document;

    /**
     * @param properties the configured content; rendered once, because it cannot change while the
     *                   application runs
     */
    public SecurityTxtValve(SecurityTxtProperties properties) {
        // Pass-through valve: it must not take asynchronous support away from the requests it
        // hands on to the next valve.
        super(true);
        this.document = properties.document().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if (!PATH.equals(request.getDecodedRequestURI()) || !isRead(request.getMethod())) {
            getNext().invoke(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentLength(document.length);
        if (!"HEAD".equals(request.getMethod())) {
            response.getOutputStream().write(document);
        }
    }

    private static boolean isRead(String method) {
        return "GET".equals(method) || "HEAD".equals(method);
    }
}
