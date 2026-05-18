package com.apiforge.domain.model;

import java.util.List;

/**
 * Aggregate root representing the fully parsed SQL schema containing tables and relational definitions.
 * 
 * @param tables        The list of parsed table schemas.
 * @param relationships The list of parsed relationship definitions between the tables.
 */
public record ParsedSchema(
    List<TableSchema> tables,
    List<RelationshipDefinition> relationships
) {
    // Return an unmodifiable copy to preserve immutability
    public ParsedSchema {
        tables = List.copyOf(tables);
        relationships = List.copyOf(relationships);
    }
}
