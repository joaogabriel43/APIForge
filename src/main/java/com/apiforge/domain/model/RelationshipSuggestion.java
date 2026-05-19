package com.apiforge.domain.model;

/**
 * Domain model representing a relationship suggestion inferred by the LLM.
 * This is a pure domain record and contains no framework or external dependencies.
 *
 * @param fromTable  The source table name.
 * @param fromColumn The foreign key column in the source table.
 * @param toTable    The target table name.
 * @param toColumn   The primary key column in the target table.
 * @param type       The type of the relationship (e.g. ManyToOne, OneToMany, OneToOne, ManyToMany).
 */
public record RelationshipSuggestion(
    String fromTable,
    String fromColumn,
    String toTable,
    String toColumn,
    String type
) {
    public RelationshipSuggestion {
        if (fromTable == null || fromTable.isBlank()) {
            throw new IllegalArgumentException("fromTable cannot be null or blank");
        }
        if (fromColumn == null || fromColumn.isBlank()) {
            throw new IllegalArgumentException("fromColumn cannot be null or blank");
        }
        if (toTable == null || toTable.isBlank()) {
            throw new IllegalArgumentException("toTable cannot be null or blank");
        }
        if (toColumn == null || toColumn.isBlank()) {
            throw new IllegalArgumentException("toColumn cannot be null or blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type cannot be null or blank");
        }
    }
}
