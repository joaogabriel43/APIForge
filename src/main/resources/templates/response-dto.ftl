package ${options.packageName}.presentation.dto;

import java.util.*;
import java.time.*;
import java.math.BigDecimal;

/**
 * Immutable DTO record representing a REST response payload for ${table.className}.
 * Generated automatically by APIForge.
 */
public record ${table.className}Response(
    UUID id<#list table.columns as col><#if !col.primaryKey && !col.foreignKey && col.name != "id" && col.name != "created_at" && col.name != "updated_at">,
    ${col.javaType} ${col.fieldName}</#if></#list><#list table.relationships as rel><#if rel.type == "ManyToOne">,
    UUID ${rel.fieldName}Id</#if></#list><#if table.hasAuditFields>,
    Instant createdAt,
    Instant updatedAt</#if>
) {}
