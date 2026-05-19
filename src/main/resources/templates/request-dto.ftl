package ${options.packageName}.presentation.dto;

import jakarta.validation.constraints.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

/**
 * Immutable DTO record representing a REST request payload for ${table.className}.
 * Generated automatically by APIForge.
 */
public record ${table.className}Request(
<#assign first = true>
<#list table.columns as col>
  <#if !col.primaryKey && !col.foreignKey && col.name != "id" && col.name != "created_at" && col.name != "updated_at">
    <#if !first>,
</#if>
    <#if !col.nullable>
      <#if col.javaType == "String">
    @NotBlank(message = "${col.fieldName} cannot be blank")
      <#else>
    @NotNull(message = "${col.fieldName} cannot be null")
      </#if>
    </#if>
    <#if col.javaType == "String" && col.size??>
    @Size(max = ${col.size}, message = "${col.fieldName} cannot exceed ${col.size} characters")
    </#if>
    ${col.javaType} ${col.fieldName}<#assign first = false>
  </#if>
</#list>
<#list table.relationships as rel>
  <#if rel.type == "ManyToOne">
    <#if !first>,
</#if>
    <#if !rel.nullable>
    @NotNull(message = "${rel.fieldName}Id cannot be null")
    </#if>
    UUID ${rel.fieldName}Id<#assign first = false>
  </#if>
</#list>
) {}
