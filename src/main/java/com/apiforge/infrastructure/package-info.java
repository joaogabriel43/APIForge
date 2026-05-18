/**
 * Infrastructure Layer (APIForge).
 * <p>
 * This layer contains all technical details and concrete implementations of adapters.
 * It houses Spring Boot configuration classes, JPA entities, database repositories,
 * Flyway migrations configuration, and external client setups.
 * </p>
 *
 * <p>
 * <b>Strict Rules:</b>
 * <ul>
 *   <li>Implements ports (interfaces) defined in the {@code domain} and {@code application} layers.</li>
 *   <li>Uses frameworks (Spring Boot, Spring Data JPA, Hibernate, P6Spy) directly here.</li>
 *   <li>Exposes configuration beans that tie the system components together.</li>
 *   <li>Does not pollute the inner layers (domain and application) with technical details.</li>
 * </ul>
 * </p>
 */
package com.apiforge.infrastructure;
