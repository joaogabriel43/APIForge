package com.apiforge.domain.service;

import com.apiforge.domain.model.TypeMapping;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Standard unit tests for the {@link SqlTypeMapper} using JUnit 5.
 * Validates successful parsing and mapping of all defined PostgreSQL types.
 */
class SqlTypeMapperTest {

    @Test
    void shouldMapUuidType() {
        TypeMapping result = SqlTypeMapper.map("UUID");
        assertEquals("java.util.UUID", result.javaType());
        assertEquals("@Column(columnDefinition = \"uuid\")", result.jpaAnnotation());

        result = SqlTypeMapper.map("uuid");
        assertEquals("java.util.UUID", result.javaType());
        assertEquals("@Column(columnDefinition = \"uuid\")", result.jpaAnnotation());
    }

    @Test
    void shouldMapVarcharAndTextTypes() {
        TypeMapping result = SqlTypeMapper.map("VARCHAR(255)");
        assertEquals("String", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());

        result = SqlTypeMapper.map("text");
        assertEquals("String", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());

        result = SqlTypeMapper.map("CHARACTER VARYING(100)");
        assertEquals("String", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());

        result = SqlTypeMapper.map("char(5)");
        assertEquals("String", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());
    }

    @Test
    void shouldMapIntegerTypes() {
        TypeMapping result = SqlTypeMapper.map("INTEGER");
        assertEquals("Integer", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());

        result = SqlTypeMapper.map("INT4");
        assertEquals("Integer", result.javaType());

        result = SqlTypeMapper.map("serial");
        assertEquals("Integer", result.javaType());

        result = SqlTypeMapper.map("SMALLINT");
        assertEquals("Integer", result.javaType());
    }

    @Test
    void shouldMapBigIntTypes() {
        TypeMapping result = SqlTypeMapper.map("BIGINT");
        assertEquals("Long", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());

        result = SqlTypeMapper.map("BIGSERIAL");
        assertEquals("Long", result.javaType());

        result = SqlTypeMapper.map("int8");
        assertEquals("Long", result.javaType());
    }

    @Test
    void shouldMapBooleanTypes() {
        TypeMapping result = SqlTypeMapper.map("BOOLEAN");
        assertEquals("Boolean", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());

        result = SqlTypeMapper.map("bool");
        assertEquals("Boolean", result.javaType());
    }

    @Test
    void shouldMapNumericTypes() {
        TypeMapping result = SqlTypeMapper.map("NUMERIC(10,2)");
        assertEquals("java.math.BigDecimal", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());

        result = SqlTypeMapper.map("decimal");
        assertEquals("java.math.BigDecimal", result.javaType());
    }

    @Test
    void shouldMapTimestampTzTypes() {
        TypeMapping result = SqlTypeMapper.map("TIMESTAMPTZ");
        assertEquals("java.time.OffsetDateTime", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());

        result = SqlTypeMapper.map("TIMESTAMP WITH TIME ZONE");
        assertEquals("java.time.OffsetDateTime", result.javaType());
        
        result = SqlTypeMapper.map("TIMESTAMP(6) WITH TIME ZONE");
        assertEquals("java.time.OffsetDateTime", result.javaType());
    }

    @Test
    void shouldMapTimestampTypes() {
        TypeMapping result = SqlTypeMapper.map("TIMESTAMP");
        assertEquals("java.time.LocalDateTime", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());

        result = SqlTypeMapper.map("TIMESTAMP WITHOUT TIME ZONE");
        assertEquals("java.time.LocalDateTime", result.javaType());

        result = SqlTypeMapper.map("TIMESTAMP(3) WITHOUT TIME ZONE");
        assertEquals("java.time.LocalDateTime", result.javaType());
    }

    @Test
    void shouldMapDateType() {
        TypeMapping result = SqlTypeMapper.map("DATE");
        assertEquals("java.time.LocalDate", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());
    }

    @Test
    void shouldMapJsonTypes() {
        TypeMapping result = SqlTypeMapper.map("JSONB");
        assertEquals("String", result.javaType());
        assertEquals("@Column(columnDefinition = \"jsonb\")", result.jpaAnnotation());

        result = SqlTypeMapper.map("json");
        assertEquals("String", result.javaType());
        assertEquals("@Column(columnDefinition = \"json\")", result.jpaAnnotation());
    }

    @Test
    void shouldMapArrayTypes() {
        TypeMapping result = SqlTypeMapper.map("TEXT[]");
        assertEquals("List<String>", result.javaType());
        assertEquals("@Column\n@JdbcTypeCode(SqlTypes.ARRAY)", result.jpaAnnotation());

        result = SqlTypeMapper.map("varchar(255)[]");
        assertEquals("List<String>", result.javaType());
        assertEquals("@Column\n@JdbcTypeCode(SqlTypes.ARRAY)", result.jpaAnnotation());
    }

    @Test
    void shouldFallbackForUnknownOrMalformedTypes() {
        TypeMapping result = SqlTypeMapper.map("GEOMETRY");
        assertEquals("String", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());

        result = SqlTypeMapper.map(null);
        assertEquals("String", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());

        result = SqlTypeMapper.map("   ");
        assertEquals("String", result.javaType());
        assertEquals("@Column", result.jpaAnnotation());
    }
}
