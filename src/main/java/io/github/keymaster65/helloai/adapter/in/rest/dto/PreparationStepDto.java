package io.github.keymaster65.helloai.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API representation of a single preparation step.
 *
 * <p>The order is derived from the position in the request list, so no explicit position
 * field is exposed on input.
 *
 * @param instruction textual instruction (required)
 */
public record PreparationStepDto(
        @NotBlank String instruction) {
}
