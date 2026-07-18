package io.github.keymaster65.helloai.adapter.in.rest;

import io.github.keymaster65.helloai.adapter.in.rest.dto.RecipeRequest;
import io.github.keymaster65.helloai.adapter.in.rest.dto.RecipeResponse;
import io.github.keymaster65.helloai.application.port.in.RecipeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST endpoints for managing recipes. The controller stays thin and delegates all
 * business logic to the {@link RecipeService} use-case port.
 */
@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeRestMapper mapper;

    public RecipeController(RecipeService recipeService, RecipeRestMapper mapper) {
        this.recipeService = recipeService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<RecipeResponse> create(
            @Valid @RequestBody RecipeRequest request, UriComponentsBuilder uriBuilder) {
        RecipeResponse created = mapper.toResponse(recipeService.create(mapper.toDomain(request)));
        URI location = uriBuilder.path("/api/recipes/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<RecipeResponse> getAll() {
        return recipeService.getAll().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public RecipeResponse getById(@PathVariable long id) {
        return mapper.toResponse(recipeService.getById(id));
    }

    @PutMapping("/{id}")
    public RecipeResponse update(@PathVariable long id, @Valid @RequestBody RecipeRequest request) {
        return mapper.toResponse(recipeService.update(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        recipeService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
