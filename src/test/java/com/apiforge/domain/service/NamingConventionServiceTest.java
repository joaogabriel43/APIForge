package com.apiforge.domain.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NamingConventionServiceTest {

    @Test
    void testToClassName() {
        assertEquals("TableName", NamingConventionService.toClassName("table_name"));
        assertEquals("UserProfile", NamingConventionService.toClassName("user_profile"));
        assertEquals("OrderItem", NamingConventionService.toClassName("order-item"));
        assertEquals("Users", NamingConventionService.toClassName("users"));
        assertEquals("Category", NamingConventionService.toClassName("category"));
        assertEquals("SomeVeryLongTableName", NamingConventionService.toClassName("some very long table name"));
        
        // Edge cases
        assertEquals("", NamingConventionService.toClassName(null));
        assertEquals("", NamingConventionService.toClassName(""));
        assertEquals("", NamingConventionService.toClassName("   "));
    }

    @Test
    void testToFieldName() {
        assertEquals("tableName", NamingConventionService.toFieldName("table_name"));
        assertEquals("userProfile", NamingConventionService.toFieldName("user_profile"));
        assertEquals("orderItem", NamingConventionService.toFieldName("order-item"));
        assertEquals("id", NamingConventionService.toFieldName("id"));
        assertEquals("someColumnName", NamingConventionService.toFieldName("some column name"));
        
        // Edge cases
        assertEquals("", NamingConventionService.toFieldName(null));
        assertEquals("", NamingConventionService.toFieldName(""));
        assertEquals("", NamingConventionService.toFieldName("   "));
    }

    @Test
    void testToRestRoute() {
        assertEquals("/users", NamingConventionService.toRestRoute("users"));
        assertEquals("/posts", NamingConventionService.toRestRoute("post"));
        assertEquals("/categories", NamingConventionService.toRestRoute("category"));
        assertEquals("/user-profiles", NamingConventionService.toRestRoute("user_profile"));
        assertEquals("/order-items", NamingConventionService.toRestRoute("order-item"));
        assertEquals("/product-catalogs", NamingConventionService.toRestRoute("product catalog"));
        
        // Edge cases
        assertEquals("/", NamingConventionService.toRestRoute(null));
        assertEquals("/", NamingConventionService.toRestRoute(""));
        assertEquals("/", NamingConventionService.toRestRoute("   "));
    }

    @Test
    void testToRelationshipFieldName() {
        assertEquals("user", NamingConventionService.toRelationshipFieldName("user_id"));
        assertEquals("category", NamingConventionService.toRelationshipFieldName("category_id"));
        assertEquals("manager", NamingConventionService.toRelationshipFieldName("managerId"));
        assertEquals("manager", NamingConventionService.toRelationshipFieldName("managerID"));
        
        // Suffix not present or other patterns
        assertEquals("user", NamingConventionService.toRelationshipFieldName("user"));
        
        // Edge cases
        assertEquals("", NamingConventionService.toRelationshipFieldName(null));
        assertEquals("", NamingConventionService.toRelationshipFieldName(""));
    }

    @Test
    void testToRelationshipClassName() {
        assertEquals("User", NamingConventionService.toRelationshipClassName("user_id"));
        assertEquals("Category", NamingConventionService.toRelationshipClassName("category_id"));
        assertEquals("Manager", NamingConventionService.toRelationshipClassName("managerId"));
        assertEquals("Manager", NamingConventionService.toRelationshipClassName("managerID"));
        
        // Suffix not present or other patterns
        assertEquals("User", NamingConventionService.toRelationshipClassName("user"));
        
        // Edge cases
        assertEquals("", NamingConventionService.toRelationshipClassName(null));
        assertEquals("", NamingConventionService.toRelationshipClassName(""));
    }
}
