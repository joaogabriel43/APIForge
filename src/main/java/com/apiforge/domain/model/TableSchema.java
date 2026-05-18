package com.apiforge.domain.model;

import java.util.List;

/**
 * Immutable record representing a database table schema.
 * 
 * @param name        The name of the table.
 * @param columns     The list of column definitions inside the table.
 * @param constraints The list of table-level constraints (e.g. "UNIQUE(code)", "CHECK (age > 18)").
 */
public record TableSchema(
    String name,
    List<ColumnDefinition> columns,
    List<String> constraints
) {
    // Return an unmodifiable copy to preserve immutability
    public TableSchema {
        columns = List.copyOf(columns);
        constraints = List.copyOf(constraints);
    }
}
