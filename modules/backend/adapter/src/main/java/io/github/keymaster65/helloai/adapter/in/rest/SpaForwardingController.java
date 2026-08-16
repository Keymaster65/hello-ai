package io.github.keymaster65.helloai.adapter.in.rest;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards the routes of the single-page application to its {@code index.html} (see docs/prompt/api.adoc).
 *
 * <p>Without this, a shared link such as {@code /recipes/3} would hit the server directly and
 * end in a 404: the browser asks for a path that only exists inside the client-side router.
 *
 * <p>The routes are listed <em>explicitly</em> rather than caught with a wildcard. A catch-all
 * would risk shadowing the API, the OpenAPI document or the Swagger UI resources; this mapping
 * cannot, because it names exactly the paths the router owns. The price is that a new route in
 * the frontend needs a line here – the corresponding system test fails if it is forgotten.
 */
@Controller
@Hidden
public class SpaForwardingController {

    private static final String INDEX = "forward:/index.html";

    /** {@code /new} – the create form. */
    @GetMapping("/new")
    public String forwardCreate() {
        return INDEX;
    }

    /** {@code /{id}} – the shareable detail view. Digits only, so it cannot swallow other paths. */
    @GetMapping("/{id:\\d+}")
    public String forwardDetail() {
        return INDEX;
    }

    /** {@code /{id}/edit} – the edit form. */
    @GetMapping("/{id:\\d+}/edit")
    public String forwardEdit() {
        return INDEX;
    }
}
