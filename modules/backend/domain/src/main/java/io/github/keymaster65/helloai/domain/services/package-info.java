/**
 * Domain services: domain logic that belongs to no single type of the model.
 *
 * <p>This package is intentionally empty for now. Every rule of the current domain fits into the
 * records of {@link io.github.keymaster65.helloai.domain.model} &ndash; a service that merely
 * forwards to a model type would only add a layer without a rule of its own.
 *
 * <p>A class belongs here when it satisfies all three conditions:
 *
 * <ul>
 *   <li>it expresses a domain rule, not a use case &ndash; orchestration, transactions and calls
 *       to ports stay in {@code application.service};
 *   <li>the rule spans several aggregates or value objects, so no single record is its owner;
 *   <li>it stays free of frameworks and of any outer ring, like the model next to it.
 * </ul>
 *
 * <p>See ADR 0020.
 */
package io.github.keymaster65.helloai.domain.services;
