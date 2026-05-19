package com.apiforge.domain.exception;

/**
 * Domain-specific runtime exception thrown when SQL schema parsing fails due to invalid syntax or structure.
 */
public class SqlParseException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     * 
     * @param message The detailed message explaining the parsing failure.
     */
    public SqlParseException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * 
     * @param message The detailed message.
     * @param cause   The root cause of the parsing failure (e.g. JSQLParser internal exception).
     */
    public SqlParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
