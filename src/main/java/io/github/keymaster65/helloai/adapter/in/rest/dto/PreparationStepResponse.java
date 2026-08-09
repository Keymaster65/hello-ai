package io.github.keymaster65.helloai.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API representation of a preparation step in a response, including its resolved order.
 *
 * @param position    1-based order of the step
 * @param instruction textual instruction
 */
@Schema(name = "PreparationStepResponse", description = "A preparation step including its resolved order")
public record PreparationStepResponse(
        @Schema(description = "1-based order of the step", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED) int position,
        @Schema(description = "Textual instruction", example = "Boil the pasta",
                requiredMode = Schema.RequiredMode.REQUIRED) String instruction) {
}
