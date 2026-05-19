package com.apiforge.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable domain record representing an audit log entry for the API code generation process.
 * Under Clean Architecture, this model is purely object-oriented and free of framework/database annotations.
 * 
 * @param id          The unique identifier of this log entry.
 * @param sqlHash     The calculated cryptographic SHA-256 hash of the original SQL schema.
 * @param packageName The target Java base package path.
 * @param options     The immutable configuration parameters used to generate the API.
 * @param fileCount   The count of files generated during this compilation.
 * @param createdAt   The timestamp of creation.
 */
public record GenerationLog(
    UUID id,
    String sqlHash,
    String packageName,
    GenerationOptions options,
    int fileCount,
    OffsetDateTime createdAt
) {
    public GenerationLog {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (sqlHash == null || sqlHash.isBlank()) {
            throw new IllegalArgumentException("sqlHash cannot be null or blank");
        }
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("packageName cannot be null or blank");
        }
        if (options == null) {
            throw new IllegalArgumentException("options cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt cannot be null");
        }
    }
}
