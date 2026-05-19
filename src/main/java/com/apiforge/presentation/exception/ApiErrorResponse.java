package com.apiforge.presentation.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standard HTTP error payload modeling the RFC 7807 (Problem Details for HTTP APIs) structure.
 * Guarantees a consistent, machine-readable, and extensible error layout across the API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    Instant timestamp,
    List<ValidationError> errors
) {
    /**
     * Nested record representing a localized validation issue inside individual fields.
     */
    public record ValidationError(
        String field,
        String message
    ) {}
}
