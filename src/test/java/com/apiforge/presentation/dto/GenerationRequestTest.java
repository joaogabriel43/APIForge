package com.apiforge.presentation.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GenerationRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldAcceptValidRequest() {
        GenerationRequest request = new GenerationRequest(
            "CREATE TABLE users (id UUID PRIMARY KEY);",
            "com.example.app",
            true,
            false,
            true
        );

        Set<ConstraintViolation<GenerationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no validation errors");
    }

    @Test
    void shouldFailWhenSqlIsBlank() {
        GenerationRequest request = new GenerationRequest(
            "  ",
            "com.example.app",
            true,
            false,
            true
        );

        Set<ConstraintViolation<GenerationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank SQL should raise validation error");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("SQL schema cannot be blank")));
    }

    @Test
    void shouldFailWhenPackageNameIsInvalid() {
        GenerationRequest request = new GenerationRequest(
            "CREATE TABLE users (id UUID PRIMARY KEY);",
            "com.Example.App-invalid",
            true,
            false,
            true
        );

        Set<ConstraintViolation<GenerationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Malformed package structure should fail");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Package name must be a valid lower-case Java package structure")));
    }

    @Test
    void shouldAssignDefaultValuesWhenBooleansAreNull() {
        GenerationRequest request = new GenerationRequest(
            "CREATE TABLE users (id UUID PRIMARY KEY);",
            "com.example.app",
            null,
            null,
            null
        );

        // Assert compact constructor applied defaults
        assertNotNull(request.generateJwt());
        assertFalse(request.generateJwt());
        
        assertNotNull(request.generatePagination());
        assertFalse(request.generatePagination());
        
        assertNotNull(request.generateSoftDelete());
        assertFalse(request.generateSoftDelete());
    }

    @Test
    void shouldCorrectlyMapToDomainOptions() {
        GenerationRequest request = new GenerationRequest(
            "CREATE TABLE users (id UUID PRIMARY KEY);",
            "com.example.app",
            true,
            null,
            true
        );

        var options = request.toOptions();
        assertEquals("com.example.app", options.packageName());
        assertTrue(options.generateJwt());
        assertFalse(options.generatePagination());
        assertTrue(options.generateSoftDelete());
    }
}
