package com.apiforge.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Aggregate root representing the combination of the original parsed schema and LLM-enriched metadata.
 * This is a pure domain record and contains no framework or external dependencies.
 *
 * @param parsedSchema         The original parsed schema.
 * @param implicitRelationships The list of implicit relationship suggestions inferred by the LLM.
 * @param domainNamesMap       Mapping of technical names (e.g. database table/column names) to clean domain names.
 * @param javadocsMap          Mapping of class/technical contexts to generated domain Javadocs.
 */
public record EnrichedSchema(
    ParsedSchema parsedSchema,
    List<RelationshipSuggestion> implicitRelationships,
    Map<String, String> domainNamesMap,
    Map<String, String> javadocsMap
) {
    public EnrichedSchema {
        if (parsedSchema == null) {
            throw new IllegalArgumentException("parsedSchema cannot be null");
        }
        implicitRelationships = List.copyOf(implicitRelationships != null ? implicitRelationships : List.of());
        domainNamesMap = Map.copyOf(domainNamesMap != null ? domainNamesMap : Map.of());
        javadocsMap = Map.copyOf(javadocsMap != null ? javadocsMap : Map.of());
    }
}
