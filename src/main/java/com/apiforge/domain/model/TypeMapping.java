package com.apiforge.domain.model;

/**
 * Immutable record representing the result of a PostgreSQL to Java/JPA type mapping.
 * 
 * @param javaType The fully-qualified or standard Java class name (e.g. "java.util.UUID", "String").
 * @param jpaAnnotation The literal JPA/Hibernate annotation string (e.g. "@Column(columnDefinition = \"uuid\")").
 */
public record TypeMapping(
    String javaType,
    String jpaAnnotation
) {}
