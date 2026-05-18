/**
 * Application Layer (APIForge).
 * <p>
 * This layer contains application-specific business rules, orchestrating the flow of data
 * to and from the domain entities. It contains the use cases, input/output boundary interfaces,
 * and command/query handlers.
 * </p>
 *
 * <p>
 * <b>Strict Rules:</b>
 * <ul>
 *   <li>Only depends on the {@code domain} layer.</li>
 *   <li>NO direct dependencies on external frameworks (Spring, Hibernate, etc.) inside the core use case logic.</li>
 *   <li>Exposes boundary interfaces (use cases) that the presentation layer calls.</li>
 *   <li>Cannot import or refer to infrastructure or presentation layers.</li>
 * </ul>
 * </p>
 */
package com.apiforge.application;
