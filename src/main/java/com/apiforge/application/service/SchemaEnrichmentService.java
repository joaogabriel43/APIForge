package com.apiforge.application.service;

import com.apiforge.domain.model.ColumnDefinition;
import com.apiforge.domain.model.EnrichedSchema;
import com.apiforge.domain.model.ParsedSchema;
import com.apiforge.domain.model.RelationshipSuggestion;
import com.apiforge.domain.model.TableSchema;
import com.apiforge.domain.repository.LlmGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrator service coordinating database schema enrichment via LLM.
 * Employs strict clean-architecture boundaries and wraps calls in protective fallback blocks
 * to ensure transient AI failures or circuit tripping do not break the core code generation workflow.
 */
@Service
public class SchemaEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(SchemaEnrichmentService.class);

    private final LlmGateway llmGateway;

    public SchemaEnrichmentService(LlmGateway llmGateway) {
        this.llmGateway = llmGateway;
    }

    /**
     * Enriches a parsed SQL schema with LLM-inferred metadata if requested.
     * Guarantees a silent fallback returning the original schema in case of downstream failures.
     *
     * @param schema          The original parsed schema.
     * @param enrichWithLlm   Toggle indicating if LLM enrichment should be processed.
     * @return The aggregate EnrichedSchema wrapper.
     */
    public EnrichedSchema enrich(ParsedSchema schema, boolean enrichWithLlm) {
        if (schema == null) {
            throw new IllegalArgumentException("schema cannot be null");
        }

        if (!enrichWithLlm) {
            return new EnrichedSchema(schema, List.of(), Map.of(), Map.of());
        }

        try {
            // 1. Infer implicit foreign key relationships
            List<RelationshipSuggestion> implicitRelationships = llmGateway.inferRelationships(schema);

            // 2. Extract technical identifiers (table names and column names) preserving order and removing duplicates
            Set<String> uniqueNames = new LinkedHashSet<>();
            List<String> classContexts = new ArrayList<>();

            for (TableSchema table : schema.tables()) {
                if (table.name() != null && !table.name().isBlank()) {
                    uniqueNames.add(table.name());
                    classContexts.add(table.name());
                }
                for (ColumnDefinition column : table.columns()) {
                    if (column.name() != null && !column.name().isBlank()) {
                        uniqueNames.add(column.name());
                    }
                }
            }

            List<String> technicalNames = new ArrayList<>(uniqueNames);

            // 3. Translate technical names to domain terms with network/guard protections
            Map<String, String> domainNamesMap = Map.of();
            if (!technicalNames.isEmpty()) {
                domainNamesMap = llmGateway.suggestDomainNames(technicalNames);
            }

            // 4. Generate Javadoc blocks with network/guard protections
            Map<String, String> javadocsMap = Map.of();
            if (!classContexts.isEmpty()) {
                javadocsMap = llmGateway.generateJavadoc(classContexts);
            }

            return new EnrichedSchema(schema, implicitRelationships, domainNamesMap, javadocsMap);

        } catch (Exception e) {
            // Rule of Gold: Catch all exceptions (timeouts, JSON errors, CircuitBreakers) and log a warning
            log.warn("LLM enrichment failed, proceeding with original schema: {}", e.getMessage(), e);
            return new EnrichedSchema(schema, List.of(), Map.of(), Map.of());
        }
    }
}
