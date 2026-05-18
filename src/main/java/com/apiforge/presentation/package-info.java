/**
 * Presentation Layer (APIForge).
 * <p>
 * This layer represents the HTTP/REST boundary, user interfaces, or API entrypoints.
 * It houses Spring MVC controllers, REST endpoints, DTO models (Request/Response),
 * model mappers, and global exception handlers.
 * </p>
 *
 * <p>
 * <b>Strict Rules:</b>
 * <ul>
 *   <li>Only calls boundary interfaces (use cases) from the {@code application} layer.</li>
 *   <li>Never accesses the domain layers directly bypassing the use cases.</li>
 *   <li>Translates REST request payloads into use case commands, and domain entities into REST DTO responses.</li>
 *   <li>Uses Spring MVC annotations (@RestController, @GetMapping, @PostMapping, etc.) to expose endpoints.</li>
 * </ul>
 * </p>
 */
package com.apiforge.presentation;
