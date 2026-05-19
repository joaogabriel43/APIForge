package com.apiforge.domain.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility domain service providing strict naming conversions and transformations 
 * for code and API generation.
 * 
 * <p>
 * This class is a pure Java domain component and is completely free of any external frameworks.
 * </p>
 */
public final class NamingConventionService {

    private NamingConventionService() {
        // Prevent instantiation
    }

    /**
     * Converts a database identifier (snake_case, kebab-case, or spaced) 
     * to a PascalCase class name (e.g. "user_profile" -> "UserProfile").
     * 
     * @param input The raw input name.
     * @return The formatted PascalCase class name, or an empty string if null/blank.
     */
    public static String toClassName(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String[] parts = input.split("[_\\-\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * Converts a database identifier (snake_case, kebab-case, or spaced) 
     * to a camelCase attribute/field name (e.g. "column_name" -> "columnName").
     * 
     * @param input The raw input name.
     * @return The formatted camelCase field name, or an empty string if null/blank.
     */
    public static String toFieldName(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String[] parts = input.split("[_\\-\\s]+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isEmpty()) {
                if (i == 0) {
                    sb.append(part.toLowerCase());
                } else {
                    sb.append(Character.toUpperCase(part.charAt(0)))
                      .append(part.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }

    /**
     * Converts a database identifier to a pluralized REST endpoint path in kebab-case, 
     * starting with a leading slash (e.g. "user_profile" -> "/user-profiles").
     * 
     * @param input The raw input name.
     * @return The pluralized REST route starting with a slash.
     */
    public static String toRestRoute(String input) {
        if (input == null || input.isBlank()) {
            return "/";
        }
        String[] parts = input.split("[_\\-\\s]+");
        List<String> kebabParts = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            if (!part.isEmpty()) {
                if (i == parts.length - 1) {
                    kebabParts.add(pluralize(part));
                } else {
                    kebabParts.add(part);
                }
            }
        }
        return "/" + String.join("-", kebabParts);
    }

    /**
     * Infers a relationship attribute name by stripping common foreign key suffixes 
     * (like _id, _Id, _ID, or camelCase Id) and returning the camelCase representation 
     * (e.g. "user_id" -> "user", "managerId" -> "manager").
     * 
     * @param fkColumnName The raw foreign key column name.
     * @return The relationship field attribute name.
     */
    public static String toRelationshipFieldName(String fkColumnName) {
        return toFieldName(stripFkSuffix(fkColumnName));
    }

    /**
     * Infers a relationship class/type name by stripping common foreign key suffixes 
     * and returning the PascalCase representation (e.g. "user_id" -> "User", "managerId" -> "Manager").
     * 
     * @param fkColumnName The raw foreign key column name.
     * @return The relationship class name.
     */
    public static String toRelationshipClassName(String fkColumnName) {
        return toClassName(stripFkSuffix(fkColumnName));
    }

    /**
     * Internal targeted English pluralization helper following basic rules 
     * to avoid massive external NLP dependencies (YAGNI).
     */
    private static String pluralize(String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        String lower = word.toLowerCase();
        // If it already ends in "s", assume it's already pluralized
        if (lower.endsWith("s")) {
            return word;
        }
        // Consonant + y -> ies (e.g. category -> categories)
        if (lower.endsWith("y") && lower.length() > 1) {
            char beforeY = lower.charAt(lower.length() - 2);
            if (beforeY != 'a' && beforeY != 'e' && beforeY != 'i' && beforeY != 'o' && beforeY != 'u') {
                return word.substring(0, word.length() - 1) + "ies";
            }
        }
        // General rule: append s
        return word + "s";
    }

    /**
     * Strips suffixes like "_id", "_Id", "_ID", or camelCase/PascalCase ending "Id/ID".
     */
    private static String stripFkSuffix(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String result = input;
        if (result.toLowerCase().endsWith("_id")) {
            result = result.substring(0, result.length() - 3);
        } else if (result.endsWith("Id")) {
            result = result.substring(0, result.length() - 2);
        } else if (result.endsWith("ID")) {
            result = result.substring(0, result.length() - 2);
        }
        return result;
    }
}
