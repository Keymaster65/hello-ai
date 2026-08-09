package io.github.keymaster65.helloai.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * API representation of a single preparation step.
 *
 * <p>The order is derived from the position in the request list, so no explicit position
 * field is exposed on input.
 *
 * @param instruction textual instruction (required)
 */
@Schema(name = "PreparationStep", description = "A single preparation step; its order is the list position")
public record PreparationStepDto(
        @Schema(description = "Textual instruction", example = "Boil the pasta")
        @NotBlank String instruction) {
}
