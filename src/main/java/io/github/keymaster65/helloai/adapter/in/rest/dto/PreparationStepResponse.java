package io.github.keymaster65.helloai.adapter.in.rest.dto;

/**
 * API representation of a preparation step in a response, including its resolved order.
 *
 * @param position    1-based order of the step
 * @param instruction textual instruction
 */
public record PreparationStepResponse(int position, String instruction) {
}
