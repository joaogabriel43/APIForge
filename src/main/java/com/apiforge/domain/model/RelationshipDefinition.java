package com.apiforge.domain.model;

/**
 * Immutable record representing a relationship definition between two database tables.
 * 
 * @param sourceTable      The name of the source (or child) table defining the relation.
 * @param targetTable      The name of the target (or parent) table referenced by the relation.
 * @param relationshipType The type of database/JPA relationship (e.g. "ManyToOne", "OneToMany", "ManyToMany").
 */
public record RelationshipDefinition(
    String sourceTable,
    String targetTable,
    String relationshipType
) {}
