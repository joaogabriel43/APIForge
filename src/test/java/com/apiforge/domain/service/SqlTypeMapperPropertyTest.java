package com.apiforge.domain.service;

import com.apiforge.domain.model.TypeMapping;
import net.jqwik.api.*;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Robust property-based tests for the {@link SqlTypeMapper} using jqwik.
 * Challenges the SQL type mapping service with arbitrary, malformed, empty,
 * or boundary-breaking strings to ensure complete exception-free resilience and fallbacks.
 */
class SqlTypeMapperPropertyTest {

    @Property
    void anyInputStringShouldNeverThrowException(@ForAll String anyString) {
        // Assert that calling map never throws any exceptions (NullPointerException, indexing errors, etc.)
        assertDoesNotThrow(() -> {
            TypeMapping mapping = SqlTypeMapper.map(anyString);
            assertNotNull(mapping);
            assertNotNull(mapping.javaType());
            assertNotNull(mapping.jpaAnnotation());
        });
    }

    @Property
    void unrecognizedOrMalformedInputsShouldFallbackToString(@ForAll("unrecognizedStrings") String unrecognized) {
        // Assert that any unrecognized or non-matching SQL type safely resolves to String and @Column
        TypeMapping result = SqlTypeMapper.map(unrecognized);
        assertEquals("String", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());
    }

    @Provide
    Arbitrary<String> unrecognizedStrings() {
        // Generate diverse, random strings, excluding standard PostgreSQL database type names
        return Arbitraries.strings()
            .filter(s -> s == null || s.trim().isEmpty() || !isRecognizedType(s));
    }

    private boolean isRecognizedType(String s) {
        String n = s.trim().toUpperCase();
        return n.startsWith("UUID") ||
               n.startsWith("VARCHAR") ||
               n.equals("TEXT") ||
               n.startsWith("CHARACTER VARYING") ||
               n.startsWith("CHAR") ||
               n.startsWith("CHARACTER") ||
               n.startsWith("BPCHAR") ||
               n.startsWith("INTEGER") ||
               n.equals("INT") ||
               n.equals("INT4") ||
               n.startsWith("SERIAL") ||
               n.startsWith("SMALLINT") ||
               n.startsWith("BIGINT") ||
               n.equals("BIGSERIAL") ||
               n.equals("INT8") ||
               n.equals("BOOLEAN") ||
               n.equals("BOOL") ||
               n.startsWith("NUMERIC") ||
               n.startsWith("DECIMAL") ||
               n.startsWith("TIMESTAMPTZ") ||
               n.contains("TIMESTAMP WITH TIME ZONE") ||
               n.startsWith("TIMESTAMP") ||
               n.equals("DATE") ||
               n.startsWith("JSONB") ||
               n.startsWith("JSON") ||
               n.endsWith("[]") ||
               n.startsWith("ARRAY");
    }
}
