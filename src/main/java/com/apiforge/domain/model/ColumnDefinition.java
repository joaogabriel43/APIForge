package com.apiforge.domain.model;

/**
 * Immutable record representing a column definition within a database table.
 * 
 * @param name             The name of the column.
 * @param originalSqlType  The exact SQL type as parsed from the DDL schema (e.g. "VARCHAR(255)").
 * @param mappedJavaType   The mapped Java type as resolved by the SqlTypeMapper service (e.g. "String").
 * @param jpaAnnotation    The literal JPA/Hibernate annotation string (e.g. "@Column").
 * @param isNullable       Indicates if the column allows NULL values.
 * @param isUnique         Indicates if the column has a UNIQUE constraint.
 * @param isPrimaryKey     Indicates if the column is a PRIMARY KEY.
 * @param isForeignKey     Indicates if the column is a FOREIGN KEY.
 */
public record ColumnDefinition(
    String name,
    String originalSqlType,
    String mappedJavaType,
    String jpaAnnotation,
    boolean isNullable,
    boolean isUnique,
    boolean isPrimaryKey,
    boolean isForeignKey
) {}
