package com.apiforge.application.service;

import com.apiforge.domain.model.ColumnDefinition;
import com.apiforge.domain.model.ParsedSchema;
import com.apiforge.domain.model.RelationshipDefinition;
import com.apiforge.domain.model.TableSchema;
import com.apiforge.domain.model.TypeMapping;
import com.apiforge.domain.service.SqlTypeMapper;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Application service responsible for parsing SQL DDL statements and 
 * converting them into domain schema definitions.
 * 
 * <p>
 * This class completely encapsulates JSQLParser. The domain layer remains 
 * pure and framework-free.
 * </p>
 */
@Component
public class SqlSchemaParser {

    public SqlSchemaParser() {
        // Public constructor for Spring bean injection
    }

    /**
     * Parses a string containing one or more SQL CREATE TABLE statements.
     * 
     * @param sql The raw SQL DDL string.
     * @return The populated {@link ParsedSchema} aggregate.
     * @throws IllegalArgumentException If parsing fails.
     */
    public static ParsedSchema parse(String sql) {
        if (sql == null || sql.isBlank()) {
            return new ParsedSchema(List.of(), List.of());
        }

        try {
            List<TableSchema> tables = new ArrayList<>();
            List<RelationshipDefinition> relationships = new ArrayList<>();

            Statements stmts = CCJSqlParserUtil.parseStatements(sql);

            for (Statement stmt : stmts.getStatements()) {
                if (stmt instanceof CreateTable createTable) {
                    // Extract table name (removing enclosing quotes/brackets)
                    String tableName = cleanIdentifier(createTable.getTable().getName());

                    List<ColumnDefinition> columns = new ArrayList<>();

                    // 1. Process individual column definitions
                    if (createTable.getColumnDefinitions() != null) {
                        for (net.sf.jsqlparser.statement.create.table.ColumnDefinition col : createTable.getColumnDefinitions()) {
                            String columnName = cleanIdentifier(col.getColumnName());
                            String originalSqlType = col.getColDataType().toString();

                            // Delegate PostgreSQL-to-Java type mapping
                            TypeMapping typeMapping = SqlTypeMapper.map(originalSqlType);
                            String mappedJavaType = typeMapping.javaType();
                            String jpaAnnotation = typeMapping.jpaAnnotation();

                            boolean isPrimaryKey = false;
                            boolean isNullable = true;
                            boolean isUnique = false;
                            boolean isForeignKey = false;
                            String refTable = null;

                            if (col.getColumnSpecs() != null) {
                                for (int i = 0; i < col.getColumnSpecs().size(); i++) {
                                    String spec = col.getColumnSpecs().get(i);

                                    // Primary Key detection
                                    if ("PRIMARY".equalsIgnoreCase(spec) && 
                                        i + 1 < col.getColumnSpecs().size() && 
                                        "KEY".equalsIgnoreCase(col.getColumnSpecs().get(i + 1))) {
                                        isPrimaryKey = true;
                                    }

                                    // Not Null detection
                                    if ("NOT".equalsIgnoreCase(spec) && 
                                        i + 1 < col.getColumnSpecs().size() && 
                                        "NULL".equalsIgnoreCase(col.getColumnSpecs().get(i + 1))) {
                                        isNullable = false;
                                    }

                                    // Unique detection
                                    if ("UNIQUE".equalsIgnoreCase(spec)) {
                                        isUnique = true;
                                    }

                                    // Inline references detection (e.g. REFERENCES users(id))
                                    if ("REFERENCES".equalsIgnoreCase(spec)) {
                                        isForeignKey = true;
                                        if (i + 1 < col.getColumnSpecs().size()) {
                                            refTable = cleanIdentifier(col.getColumnSpecs().get(i + 1));
                                        }
                                    }
                                }
                            }

                            // A primary key is implicitly NOT NULL
                            if (isPrimaryKey) {
                                isNullable = false;
                            }

                            // If inline FK relationship is found, infer bi-directional relationships
                            if (isForeignKey && refTable != null) {
                                relationships.add(new RelationshipDefinition(tableName, refTable, "ManyToOne"));
                                relationships.add(new RelationshipDefinition(refTable, tableName, "OneToMany"));
                            }

                            columns.add(new ColumnDefinition(
                                columnName,
                                originalSqlType,
                                mappedJavaType,
                                jpaAnnotation,
                                isNullable,
                                isUnique,
                                isPrimaryKey,
                                isForeignKey
                            ));
                        }
                    }

                    // 2. Process table-level constraints (like FOREIGN KEY, UNIQUE, etc.)
                    List<String> constraints = new ArrayList<>();
                    if (createTable.getIndexes() != null) {
                        for (Index idx : createTable.getIndexes()) {
                            constraints.add(idx.toString());

                            // Detect table-level ForeignKey constraints
                            if (idx instanceof ForeignKeyIndex fkIdx) {
                                String targetTable = cleanIdentifier(fkIdx.getTable().getName());

                                // Infer bi-directional relationships
                                relationships.add(new RelationshipDefinition(tableName, targetTable, "ManyToOne"));
                                relationships.add(new RelationshipDefinition(targetTable, tableName, "OneToMany"));

                                // Update matching column definitions to isForeignKey = true
                                List<String> fkColNames = fkIdx.getColumnsNames();
                                if (fkColNames != null) {
                                    for (int i = 0; i < columns.size(); i++) {
                                        ColumnDefinition c = columns.get(i);
                                        // Match ignoring case and identifier symbols
                                        boolean matches = fkColNames.stream()
                                            .anyMatch(colName -> cleanIdentifier(colName).equalsIgnoreCase(c.name()));
                                        if (matches) {
                                            columns.set(i, new ColumnDefinition(
                                                c.name(),
                                                c.originalSqlType(),
                                                c.mappedJavaType(),
                                                c.jpaAnnotation(),
                                                c.isNullable(),
                                                c.isUnique(),
                                                c.isPrimaryKey(),
                                                true // isForeignKey = true
                                            ));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    tables.add(new TableSchema(tableName, columns, constraints));
                }
            }

            return new ParsedSchema(tables, relationships);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse SQL schema: " + e.getMessage(), e);
        }
    }

    private static String cleanIdentifier(String raw) {
        if (raw == null) {
            return null;
        }
        // Remove schema prefix (e.g. "public.users" -> "users")
        String result = raw;
        if (result.contains(".")) {
            result = result.substring(result.lastIndexOf(".") + 1);
        }
        // Strip out enclosing quotes or SQL brackets (e.g. `users`, "users", [users])
        return result.replaceAll("[\"`\\[\\]]", "").trim();
    }
}
