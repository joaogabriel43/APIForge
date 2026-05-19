package com.apiforge.application.service;

import com.apiforge.domain.model.ColumnDefinition;
import com.apiforge.domain.model.GenerationOptions;
import com.apiforge.domain.model.ParsedSchema;
import com.apiforge.domain.model.RelationshipDefinition;
import com.apiforge.domain.model.TableSchema;
import com.apiforge.domain.service.NamingConventionService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.*;

/**
 * Service responsible for orchestrating the code generation process.
 * 
 * <p>
 * This class coordinates the mapping from parsed schemas into context models,
 * then invokes the FreeMarker template engine to render all layers of the generated application.
 * </p>
 */
@Service
public class CodeGenerationService {

    private final Configuration freemarkerConfig;

    public CodeGenerationService() {
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
        this.freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/templates");
        this.freemarkerConfig.setDefaultEncoding("UTF-8");
    }

    /**
     * Generates source code and configuration files for all application layers.
     * 
     * @param schema  The parsed relational schema.
     * @param options Target packages and parameters for code generation.
     * @return A map of logical/relative file paths to rendered source code contents.
     */
    public Map<String, String> generate(ParsedSchema schema, GenerationOptions options) {
        Map<String, String> generatedFiles = new HashMap<>();
        String pkgPath = options.packageName().replace('.', '/');

        // 1. Process and generate per-table source code files
        for (TableSchema tableSchema : schema.tables()) {
            Map<String, Object> context = buildTableContext(tableSchema, schema.relationships(), options);

            @SuppressWarnings("unchecked")
            Map<String, Object> tableMap = (Map<String, Object>) context.get("table");
            String className = (String) tableMap.get("className");
            String tableName = tableSchema.name();

            generatedFiles.put("src/main/java/" + pkgPath + "/domain/entity/" + className + ".java",
                    renderTemplate("entity.ftl", context));
            generatedFiles.put("src/main/java/" + pkgPath + "/infrastructure/repository/" + className + "Repository.java",
                    renderTemplate("repository.ftl", context));
            generatedFiles.put("src/main/java/" + pkgPath + "/application/service/" + className + "Service.java",
                    renderTemplate("service.ftl", context));
            generatedFiles.put("src/main/java/" + pkgPath + "/presentation/controller/" + className + "Controller.java",
                    renderTemplate("controller.ftl", context));
            generatedFiles.put("src/main/java/" + pkgPath + "/presentation/dto/" + className + "Request.java",
                    renderTemplate("request-dto.ftl", context));
            generatedFiles.put("src/main/java/" + pkgPath + "/presentation/dto/" + className + "Response.java",
                    renderTemplate("response-dto.ftl", context));
            generatedFiles.put("src/main/java/" + pkgPath + "/presentation/mapper/" + className + "Mapper.java",
                    renderTemplate("mapper.ftl", context));
            generatedFiles.put("src/main/resources/db/migration/V1__create_" + tableName + "_table.sql",
                    renderTemplate("flyway-migration.ftl", context));
        }

        // 2. Generate project-wide configuration and build files
        if (!schema.tables().isEmpty()) {
            TableSchema firstTable = schema.tables().get(0);
            Map<String, Object> context = buildTableContext(firstTable, schema.relationships(), options);

            generatedFiles.put("docker-compose.yml", renderTemplate("docker-compose.ftl", context));
            generatedFiles.put("src/main/resources/application.properties", renderTemplate("application-properties.ftl", context));
            generatedFiles.put("pom.xml", renderTemplate("pom.ftl", context));
        }

        return generatedFiles;
    }

    private String renderTemplate(String templateName, Map<String, Object> context) {
        try {
            Template template = freemarkerConfig.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(context, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render FreeMarker template: " + templateName, e);
        }
    }

    private Map<String, Object> buildTableContext(TableSchema tableSchema, List<RelationshipDefinition> allRelationships, GenerationOptions options) {
        Map<String, Object> context = new HashMap<>();
        context.put("options", options);

        Map<String, Object> tableMap = new HashMap<>();
        tableMap.put("name", tableSchema.name());
        tableMap.put("className", NamingConventionService.toClassName(tableSchema.name()));
        tableMap.put("fieldName", NamingConventionService.toFieldName(tableSchema.name()));
        tableMap.put("restRoute", NamingConventionService.toRestRoute(tableSchema.name()));

        // Map column details to template properties
        List<Map<String, Object>> columnsList = new ArrayList<>();
        for (ColumnDefinition col : tableSchema.columns()) {
            Map<String, Object> colMap = new HashMap<>();
            colMap.put("name", col.name());
            colMap.put("fieldName", NamingConventionService.toFieldName(col.name()));
            colMap.put("originalSqlType", col.originalSqlType());
            colMap.put("javaType", col.mappedJavaType());
            colMap.put("jpaAnnotation", col.jpaAnnotation());
            colMap.put("nullable", col.isNullable());
            colMap.put("unique", col.isUnique());
            colMap.put("primaryKey", col.isPrimaryKey());
            colMap.put("foreignKey", col.isForeignKey());
            columnsList.add(colMap);
        }
        tableMap.put("columns", columnsList);

        // Detect auto audit fields
        boolean hasCreatedAt = tableSchema.columns().stream().anyMatch(c -> c.name().equalsIgnoreCase("created_at"));
        boolean hasUpdatedAt = tableSchema.columns().stream().anyMatch(c -> c.name().equalsIgnoreCase("updated_at"));
        tableMap.put("hasAuditFields", hasCreatedAt && hasUpdatedAt);

        // Process bidirectional relationship properties
        List<Map<String, Object>> relationshipsList = new ArrayList<>();
        for (RelationshipDefinition rel : allRelationships) {
            Map<String, Object> relMap = new HashMap<>();
            if (rel.relationshipType().equalsIgnoreCase("ManyToOne") && rel.sourceTable().equalsIgnoreCase(tableSchema.name())) {
                relMap.put("type", "ManyToOne");
                
                String fkColumn = findFkColumn(tableSchema, rel.targetTable());
                boolean isFkNullable = true;
                for (ColumnDefinition col : tableSchema.columns()) {
                    if (col.name().equalsIgnoreCase(fkColumn)) {
                        isFkNullable = col.isNullable();
                        break;
                    }
                }
                
                relMap.put("fkColumnName", fkColumn);
                relMap.put("nullable", isFkNullable);
                relMap.put("targetClassName", NamingConventionService.toClassName(rel.targetTable()));
                relMap.put("targetTableName", rel.targetTable());
                relMap.put("fieldName", NamingConventionService.toRelationshipFieldName(fkColumn));
                relationshipsList.add(relMap);
                
            } else if (rel.relationshipType().equalsIgnoreCase("OneToMany") && rel.sourceTable().equalsIgnoreCase(tableSchema.name())) {
                relMap.put("type", "OneToMany");
                relMap.put("targetClassName", NamingConventionService.toClassName(rel.targetTable()));
                
                // Formulate target plural collection property (e.g. "posts")
                String pluralRoute = NamingConventionService.toRestRoute(rel.targetTable());
                String pluralName = pluralRoute.startsWith("/") ? pluralRoute.substring(1) : pluralRoute;
                relMap.put("fieldName", NamingConventionService.toFieldName(pluralName));
                
                // Mapped by inverse property name
                String inverseFk = targetSingular(tableSchema.name()) + "_id";
                relMap.put("mappedByFieldName", NamingConventionService.toRelationshipFieldName(inverseFk));
                relationshipsList.add(relMap);
            }
        }
        tableMap.put("relationships", relationshipsList);

        context.put("table", tableMap);
        return context;
    }

    private static String findFkColumn(TableSchema sourceTable, String targetTableName) {
        String targetSingular = targetSingular(targetTableName);
        String targetLower = targetTableName.toLowerCase();

        for (ColumnDefinition col : sourceTable.columns()) {
            if (col.isForeignKey()) {
                String nameLower = col.name().toLowerCase();
                String stripped = nameLower;
                if (nameLower.endsWith("_id")) {
                    stripped = nameLower.substring(0, nameLower.length() - 3);
                } else if (nameLower.endsWith("id")) {
                    stripped = nameLower.substring(0, nameLower.length() - 2);
                }
                
                stripped = stripped.replaceAll("[_\\-]", "");
                String matchTarget = targetLower.replaceAll("[_\\-]", "");
                String matchSingular = targetSingular.replaceAll("[_\\-]", "");
                
                if (stripped.equals(matchTarget) || stripped.equals(matchSingular) || matchTarget.contains(stripped) || stripped.contains(matchSingular)) {
                    return col.name();
                }
            }
        }

        // Return first matching FK fallback if not matched by name singulars
        for (ColumnDefinition col : sourceTable.columns()) {
            if (col.isForeignKey()) {
                return col.name();
            }
        }
        return targetSingular + "_id";
    }

    private static String targetSingular(String tableName) {
        String lower = tableName.toLowerCase();
        if (lower.endsWith("ies") && lower.length() > 3) {
            return lower.substring(0, lower.length() - 3) + "y";
        } else if (lower.endsWith("s") && lower.length() > 1) {
            return lower.substring(0, lower.length() - 1);
        }
        return lower;
    }
}
