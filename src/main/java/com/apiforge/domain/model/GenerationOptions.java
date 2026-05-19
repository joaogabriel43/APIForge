package com.apiforge.domain.model;

/**
 * Immutable configuration options holding parameters for the API code generation process.
 * 
 * @param packageName         The target base package for generated classes (e.g. "com.example.myapp").
 * @param generateJwt         Whether to generate JWT-based authentication and security files.
 * @param generatePagination  Whether to generate pagination and sorting capabilities inside repositories and controllers.
 * @param generateSoftDelete  Whether to generate soft delete annotations and queries for database entities.
 */
public record GenerationOptions(
    String packageName,
    boolean generateJwt,
    boolean generatePagination,
    boolean generateSoftDelete
) {
    public GenerationOptions {
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("packageName cannot be null or blank");
        }
    }
}
