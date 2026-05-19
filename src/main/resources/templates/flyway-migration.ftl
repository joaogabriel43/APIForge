-- Flyway Migration: V1__create_${table.name}_table.sql
-- Generated automatically by APIForge.

CREATE TABLE ${table.name} (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid()<#list table.columns as col><#if !col.primaryKey && col.name != "id">,
    ${col.name} ${col.originalSqlType}<#if !col.nullable> NOT NULL</#if><#if col.unique> UNIQUE</#if></#if></#list><#list table.relationships as rel><#if rel.type == "ManyToOne">,
    ${rel.fkColumnName} UUID<#if !rel.nullable> NOT NULL</#if> REFERENCES ${rel.targetTableName}(id)</#if></#list>
);

-- Foreign Key Column Performance Indexes
<#list table.relationships as rel>
  <#if rel.type == "ManyToOne">
CREATE INDEX idx_${table.name}_${rel.fkColumnName} ON ${table.name}(${rel.fkColumnName});
  </#if>
</#list>
