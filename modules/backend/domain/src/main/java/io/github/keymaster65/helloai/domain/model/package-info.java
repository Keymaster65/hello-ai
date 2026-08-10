/**
 * The domain model: entities, aggregates and value objects.
 *
 * <p>Everything here is a plain Java record or enum without any framework dependency. The types
 * carry their own invariants in their compact constructors, so an instance that exists is a valid
 * one &ndash; validation is not deferred to a service.
 *
 * <p>Behaviour that spans several aggregates and therefore has no natural home on a single type
 * belongs in {@link io.github.keymaster65.helloai.domain.services} instead.
 */
package io.github.keymaster65.helloai.domain.model;
