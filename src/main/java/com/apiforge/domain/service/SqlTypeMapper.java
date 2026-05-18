package com.apiforge.domain.service;

import com.apiforge.domain.model.TypeMapping;
import java.util.logging.Logger;

/**
 * Domain service responsible for mapping PostgreSQL database types to Java classes 
 * and their corresponding JPA/Hibernate annotation representations.
 * 
 * <p>
 * Implements strict Clean Architecture principles: no dependencies on Spring or JPA frameworks.
 * All annotations are returned as String literals.
 * </p>
 */
public final class SqlTypeMapper {
    private static final Logger LOGGER = Logger.getLogger(SqlTypeMapper.class.getName());

    private SqlTypeMapper() {
        // Prevent instantiation
    }

    /**
     * Maps a PostgreSQL SQL type declaration to its Java/JPA mapping configuration.
     * 
     * @param sqlType The raw PostgreSQL SQL type (e.g. "VARCHAR(255)", "TIMESTAMPTZ", "TEXT[]").
     * @return The resolved {@link TypeMapping}. Never throws an exception.
     */
    public static TypeMapping map(String sqlType) {
        if (sqlType == null || sqlType.isBlank()) {
            LOGGER.warning("SQL type is null or blank. Falling back to String mapping.");
            return new TypeMapping("String", "@Column");
        }

        // Normalize string
        String normalized = sqlType.trim().toUpperCase();

        // 1. Check for PostgreSQL array types (e.g., TEXT[], INT[]) or explicit ARRAY declarations
        if (normalized.endsWith("[]") || normalized.startsWith("ARRAY")) {
            return new TypeMapping("List<String>", "@Column\n@JdbcTypeCode(SqlTypes.ARRAY)");
        }

        // 2. Extract base type by stripping parameters inside parentheses (e.g., "VARCHAR(255)" -> "VARCHAR")
        String baseType = normalized.replaceAll("\\(.*\\)", "").trim();

        // 3. Match against supported PostgreSQL types
        switch (baseType) {
            case "UUID":
                return new TypeMapping("java.util.UUID", "@Column(columnDefinition = \"uuid\")");

            case "VARCHAR":
            case "TEXT":
            case "CHARACTER VARYING":
            case "CHAR":
            case "CHARACTER":
            case "BPCHAR": // PostgreSQL internal blank-padded char
                return new TypeMapping("String", "@Column");

            case "INTEGER":
            case "INT":
            case "INT4":
            case "SERIAL":
            case "SMALLINT":
            case "INT2":
            case "SMALLSERIAL":
                return new TypeMapping("Integer", "@Column");

            case "BIGINT":
            case "INT8":
            case "BIGSERIAL":
                return new TypeMapping("Long", "@Column");

            case "BOOLEAN":
            case "BOOL":
                return new TypeMapping("Boolean", "@Column");

            case "NUMERIC":
            case "DECIMAL":
            case "MONEY":
                return new TypeMapping("java.math.BigDecimal", "@Column");

            case "TIMESTAMPTZ":
            case "TIMESTAMP WITH TIME ZONE":
                return new TypeMapping("java.time.OffsetDateTime", "@Column");

            case "TIMESTAMP":
            case "TIMESTAMP WITHOUT TIME ZONE":
                return new TypeMapping("java.time.LocalDateTime", "@Column");

            case "DATE":
                return new TypeMapping("java.time.LocalDate", "@Column");

            case "JSONB":
                return new TypeMapping("String", "@Column(columnDefinition = \"jsonb\")");

            case "JSON":
                return new TypeMapping("String", "@Column(columnDefinition = \"json\")");

            default:
                // Handle composite variations of TIMESTAMP WITH TIME ZONE that regex stripping might miss
                if (normalized.contains("TIMESTAMP") && normalized.contains("WITH TIME ZONE")) {
                    return new TypeMapping("java.time.OffsetDateTime", "@Column");
                }
                if (normalized.contains("TIMESTAMP") && normalized.contains("WITHOUT TIME ZONE")) {
                    return new TypeMapping("java.time.LocalDateTime", "@Column");
                }

                LOGGER.warning("Unknown/unsupported SQL type: '" + sqlType + "'. Falling back to String mapping.");
                return new TypeMapping("String", "@Column");
        }
    }
}
