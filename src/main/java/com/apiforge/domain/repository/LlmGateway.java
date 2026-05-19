package com.apiforge.domain.repository;

import com.apiforge.domain.model.ParsedSchema;
import com.apiforge.domain.model.RelationshipSuggestion;

import java.util.List;
import java.util.Map;

/**
 * Pure domain port interface defining the LLM enrichment capabilities.
 * Decouples core business logic from concrete AI services or client-side resilience components.
 */
public interface LlmGateway {

    /**
     * Infers implicit, non-declared relationships between tables based on the parsed schema.
     *
     * @param schema The original parsed schema.
     * @return A list of suggested relationships.
     */
    List<RelationshipSuggestion> inferRelationships(ParsedSchema schema);

    /**
     * Translates technical names (database snake_case names) to rich domain-specific entity/property names.
     *
     * @param technicalNames A list of technical names.
     * @return A mapping of technical names to suggested clean domain names.
     */
    Map<String, String> suggestDomainNames(List<String> technicalNames);

    /**
     * Generates descriptive Javadoc blocks for class structures and components using context descriptions.
     *
     * @param classContexts A list of descriptive contexts for classes.
     * @return A mapping of class/technical context keys to generated Javadocs.
     */
    Map<String, String> generateJavadoc(List<String> classContexts);
}
