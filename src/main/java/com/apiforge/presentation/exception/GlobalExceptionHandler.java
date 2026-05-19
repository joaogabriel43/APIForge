package com.apiforge.presentation.exception;

import com.apiforge.domain.exception.SqlParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized global exception handler mapping runtime failures into RFC 7807 standard responses.
 * Guarantees that internal stack traces are strictly kept inside server logs and never exposed to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Intercepts Spring Bean Validation errors (MethodArgumentNotValidException).
     * Returns HTTP 400 Bad Request with a detailed field-by-field array.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Payload validation failed on request {}: {}", getRequestUri(request), ex.getMessage());
        
        List<ApiErrorResponse.ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ApiErrorResponse.ValidationError(err.getField(), err.getDefaultMessage()))
                .collect(Collectors.toList());

        ApiErrorResponse response = new ApiErrorResponse(
                "https://apiforge.com/errors/validation-failed",
                "Validation Failed",
                HttpStatus.BAD_REQUEST.value(),
                "The request payload failed structural validation rules.",
                getRequestUri(request),
                Instant.now(),
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Intercepts custom SQL Schema parsing and dialect failures (SqlParseException).
     * Returns HTTP 422 Unprocessable Entity with the exact domain explanation message.
     */
    @ExceptionHandler(SqlParseException.class)
    public ResponseEntity<ApiErrorResponse> handleSqlParseException(SqlParseException ex, WebRequest request) {
        log.error("SQL schema parsing failure on request {}: {}", getRequestUri(request), ex.getMessage(), ex);

        ApiErrorResponse response = new ApiErrorResponse(
                "https://apiforge.com/errors/unprocessable-schema",
                "Unprocessable Schema",
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getMessage(),
                getRequestUri(request),
                Instant.now(),
                null
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    /**
     * Fallback general exception handler matching unexpected Java/Spring exceptions (Exception).
     * Returns HTTP 500 Internal Server Error with a highly sanitized, cryptographically secure warning.
     * Prevents technical database structure or programming languages leaks.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        // Log the stack trace internally for engineering diagnostics
        log.error("Internal server error encountered on request {}: ", getRequestUri(request), ex);

        ApiErrorResponse response = new ApiErrorResponse(
                "https://apiforge.com/errors/internal-server-error",
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected system error occurred. Please contact the administrator.",
                getRequestUri(request),
                Instant.now(),
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String getRequestUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return "unknown";
    }
}
