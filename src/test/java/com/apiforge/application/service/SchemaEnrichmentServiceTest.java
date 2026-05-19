package com.apiforge.application.service;

import com.apiforge.domain.model.ColumnDefinition;
import com.apiforge.domain.model.EnrichedSchema;
import com.apiforge.domain.model.ParsedSchema;
import com.apiforge.domain.model.RelationshipSuggestion;
import com.apiforge.domain.model.TableSchema;
import com.apiforge.domain.repository.LlmGateway;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Fast Mockito unit tests for {@link SchemaEnrichmentService}.
 * Verifies disabled paths, successful merging, and robust silent fallbacks in case of transient exceptions or tripped circuit breakers.
 */
@ExtendWith(MockitoExtension.class)
class SchemaEnrichmentServiceTest {

    @Mock
    private LlmGateway llmGateway;

    private SchemaEnrichmentService enrichmentService;

    private ParsedSchema originalSchema;

    @BeforeEach
    void setUp() {
        enrichmentService = new SchemaEnrichmentService(llmGateway);

        // Setup a parsed schema containing table definitions
        ColumnDefinition col1 = new ColumnDefinition("usr_id", "INT", "Integer", "@Id", false, false, true, false);
        ColumnDefinition col2 = new ColumnDefinition("usr_name", "VARCHAR(50)", "String", "@Column", true, false, false, false);
        TableSchema table = new TableSchema("users", List.of(col1, col2), List.of());
        originalSchema = new ParsedSchema(List.of(table), List.of());
    }

    @Test
    void shouldReturnOriginalSchemaImmediatelyAndBypassGatewayInteractionsWhenDisabled() {
        // Scenario 1 (Disabled): enrichWithLlm = false
        EnrichedSchema result = enrichmentService.enrich(originalSchema, false);

        assertNotNull(result);
        assertEquals(originalSchema, result.parsedSchema());
        assertTrue(result.implicitRelationships().isEmpty());
        assertTrue(result.domainNamesMap().isEmpty());
        assertTrue(result.javadocsMap().isEmpty());

        verifyNoInteractions(llmGateway);
    }

    @Test
    void shouldSuccessfullyMergeLlmMetadataWhenEnabledAndGatewaySucceeds() {
        // Scenario 2 (Success): enrichWithLlm = true and LLM gateway answers in green
        List<RelationshipSuggestion> mockSuggestions = List.of(
                new RelationshipSuggestion("orders", "user_id", "users", "id", "ManyToOne")
        );
        Map<String, String> mockNaming = Map.of("usr_id", "id", "users", "User");
        Map<String, String> mockJavadocs = Map.of("users", "/** User Javadoc */");

        when(llmGateway.inferRelationships(any(ParsedSchema.class))).thenReturn(mockSuggestions);
        when(llmGateway.suggestDomainNames(anyList())).thenReturn(mockNaming);
        when(llmGateway.generateJavadoc(anyList())).thenReturn(mockJavadocs);

        EnrichedSchema result = enrichmentService.enrich(originalSchema, true);

        assertNotNull(result);
        assertEquals(originalSchema, result.parsedSchema());
        assertEquals(1, result.implicitRelationships().size());
        assertEquals("orders", result.implicitRelationships().get(0).fromTable());
        assertEquals("User", result.domainNamesMap().get("users"));
        assertEquals("/** User Javadoc */", result.javadocsMap().get("users"));

        verify(llmGateway, times(1)).inferRelationships(any(ParsedSchema.class));
        verify(llmGateway, times(1)).suggestDomainNames(anyList());
        verify(llmGateway, times(1)).generateJavadoc(anyList());
    }

    @Test
    void shouldPerformSilentFallbackAndReturnOriginalSchemaWhenGatewayThrowsTransientRuntimeException() {
        // Scenario 3 (Transient network error): Mock throws RuntimeException -> caught and silences
        when(llmGateway.inferRelationships(any(ParsedSchema.class))).thenThrow(new RuntimeException("Transient OpenAI API Timeout"));

        EnrichedSchema result = enrichmentService.enrich(originalSchema, true);

        assertNotNull(result);
        assertEquals(originalSchema, result.parsedSchema());
        assertTrue(result.implicitRelationships().isEmpty());
        assertTrue(result.domainNamesMap().isEmpty());
        assertTrue(result.javadocsMap().isEmpty());

        // Assert that the calls stop immediately after the first failure is caught
        verify(llmGateway, times(1)).inferRelationships(any(ParsedSchema.class));
        verify(llmGateway, never()).suggestDomainNames(anyList());
        verify(llmGateway, never()).generateJavadoc(anyList());
    }

    @Test
    void shouldPerformSilentFallbackAndReturnOriginalSchemaWhenCircuitBreakerIsTrippedAndThrowsCallNotPermittedException() {
        // Scenario 4 (Tripped Circuit Breaker): Mock throws CallNotPermittedException -> caught and silences
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("openai");
        CallNotPermittedException trippedException = CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        when(llmGateway.inferRelationships(any(ParsedSchema.class))).thenThrow(trippedException);

        EnrichedSchema result = enrichmentService.enrich(originalSchema, true);

        assertNotNull(result);
        assertEquals(originalSchema, result.parsedSchema());
        assertTrue(result.implicitRelationships().isEmpty());
        assertTrue(result.domainNamesMap().isEmpty());
        assertTrue(result.javadocsMap().isEmpty());

        verify(llmGateway, times(1)).inferRelationships(any(ParsedSchema.class));
        verify(llmGateway, never()).suggestDomainNames(anyList());
        verify(llmGateway, never()).generateJavadoc(anyList());
    }
}
