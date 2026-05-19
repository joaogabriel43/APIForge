package com.apiforge.application.service;

import com.apiforge.domain.model.ColumnDefinition;
import com.apiforge.domain.model.ParsedSchema;
import com.apiforge.domain.model.RelationshipDefinition;
import com.apiforge.domain.model.TableSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test suite for the {@link SqlSchemaParser}.
 * 
 * <p>
 * Uses Testcontainers to run a real PostgreSQL container instance, 
 * verifying that our schemas comply with the actual PostgreSQL dialect 
 * before running assertions on the parsed domain output.
 * </p>
 */
@Testcontainers
@EnabledIf("isDockerOnline")
class SqlSchemaParserIntegrationTest {

    private static boolean isDockerOnline() {
        try {
            return org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    // Efficient Testcontainers lifecycle: Static container starts exactly once for all tests in this class
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("apiforge")
            .withUsername("apiforge")
            .withPassword("apiforge");

    @BeforeEach
    void cleanDatabase() throws Exception {
        // Drop all tables before each test to ensure transaction and DB state isolation
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS products;");
            stmt.execute("DROP TABLE IF EXISTS categories;");
            stmt.execute("DROP TABLE IF EXISTS posts;");
            stmt.execute("DROP TABLE IF EXISTS users;");
            stmt.execute("DROP TABLE IF EXISTS events;");
        }
    }

    @Test
    void shouldValidateAndParseSchema1Basic() throws Exception {
        String ddl = """
            CREATE TABLE users (
                id UUID PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                created_at TIMESTAMPTZ NOT NULL
            );
            """;

        // 1. Physically execute DDL inside PostgreSQL to prove absolute dialect compliance
        executeSql(ddl);

        // 2. Parse the schema in-memory
        ParsedSchema parsed = SqlSchemaParser.parse(ddl);

        // 3. Complete structural assertions
        assertNotNull(parsed);
        assertEquals(1, parsed.tables().size());
        assertTrue(parsed.relationships().isEmpty());

        TableSchema usersTable = parsed.tables().get(0);
        assertEquals("users", usersTable.name());
        assertEquals(3, usersTable.columns().size());

        // id UUID PRIMARY KEY
        ColumnDefinition idCol = usersTable.columns().stream()
                .filter(c -> "id".equalsIgnoreCase(c.name())).findFirst().orElseThrow();
        assertEquals("java.util.UUID", idCol.mappedJavaType());
        assertEquals("@Column(columnDefinition = \"uuid\")", idCol.jpaAnnotation());
        assertTrue(idCol.isPrimaryKey());
        assertFalse(idCol.isNullable());
        assertFalse(idCol.isForeignKey());

        // name VARCHAR(255) NOT NULL
        ColumnDefinition nameCol = usersTable.columns().stream()
                .filter(c -> "name".equalsIgnoreCase(c.name())).findFirst().orElseThrow();
        assertEquals("String", nameCol.mappedJavaType());
        assertEquals("@Column", nameCol.jpaAnnotation());
        assertFalse(nameCol.isPrimaryKey());
        assertFalse(nameCol.isNullable());

        // created_at TIMESTAMPTZ NOT NULL
        ColumnDefinition createdAtCol = usersTable.columns().stream()
                .filter(c -> "created_at".equalsIgnoreCase(c.name())).findFirst().orElseThrow();
        assertEquals("java.time.OffsetDateTime", createdAtCol.mappedJavaType());
        assertEquals("@Column", createdAtCol.jpaAnnotation());
        assertFalse(createdAtCol.isPrimaryKey());
        assertFalse(createdAtCol.isNullable());
    }

    @Test
    void shouldValidateAndParseSchema2FkInline() throws Exception {
        String ddl = """
            CREATE TABLE users (
                id UUID PRIMARY KEY,
                name VARCHAR(255) NOT NULL
            );
            CREATE TABLE posts (
                id UUID PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                user_id UUID REFERENCES users(id)
            );
            """;

        // 1. Execute SQL inside PostgreSQL container
        executeSql(ddl);

        // 2. Parse DDL in-memory
        ParsedSchema parsed = SqlSchemaParser.parse(ddl);

        // 3. Complete structural assertions
        assertNotNull(parsed);
        assertEquals(2, parsed.tables().size());

        TableSchema usersTable = parsed.tables().stream()
                .filter(t -> "users".equalsIgnoreCase(t.name())).findFirst().orElseThrow();
        TableSchema postsTable = parsed.tables().stream()
                .filter(t -> "posts".equalsIgnoreCase(t.name())).findFirst().orElseThrow();

        // Verify users columns
        assertEquals(2, usersTable.columns().size());

        // Verify posts columns & ForeignKey marker
        assertEquals(3, postsTable.columns().size());
        ColumnDefinition userIdCol = postsTable.columns().stream()
                .filter(c -> "user_id".equalsIgnoreCase(c.name())).findFirst().orElseThrow();
        assertTrue(userIdCol.isForeignKey());
        assertEquals("java.util.UUID", userIdCol.mappedJavaType());

        // Verify bi-directional relationship inference (ManyToOne and OneToMany)
        assertEquals(2, parsed.relationships().size());

        RelationshipDefinition rel1 = parsed.relationships().stream()
                .filter(r -> "posts".equals(r.sourceTable())).findFirst().orElseThrow();
        assertEquals("users", rel1.targetTable());
        assertEquals("ManyToOne", rel1.relationshipType());

        RelationshipDefinition rel2 = parsed.relationships().stream()
                .filter(r -> "users".equals(r.sourceTable())).findFirst().orElseThrow();
        assertEquals("posts", rel2.targetTable());
        assertEquals("OneToMany", rel2.relationshipType());
    }

    @Test
    void shouldValidateAndParseSchema3AdvancedTypes() throws Exception {
        String ddl = """
            CREATE TABLE events (
                id BIGSERIAL PRIMARY KEY,
                payload JSONB NOT NULL,
                tags TEXT[] NOT NULL,
                amount NUMERIC(10,2) NOT NULL,
                event_date DATE NOT NULL
            );
            """;

        // 1. Execute SQL inside PostgreSQL container
        executeSql(ddl);

        // 2. Parse DDL in-memory
        ParsedSchema parsed = SqlSchemaParser.parse(ddl);

        // 3. Complete structural assertions
        assertNotNull(parsed);
        assertEquals(1, parsed.tables().size());
        assertTrue(parsed.relationships().isEmpty());

        TableSchema eventsTable = parsed.tables().get(0);
        assertEquals("events", eventsTable.name());
        assertEquals(5, eventsTable.columns().size());

        // id BIGSERIAL PRIMARY KEY
        ColumnDefinition idCol = eventsTable.columns().stream()
                .filter(c -> "id".equalsIgnoreCase(c.name())).findFirst().orElseThrow();
        assertEquals("Long", idCol.mappedJavaType());
        assertTrue(idCol.isPrimaryKey());

        // payload JSONB NOT NULL
        ColumnDefinition payloadCol = eventsTable.columns().stream()
                .filter(c -> "payload".equalsIgnoreCase(c.name())).findFirst().orElseThrow();
        assertEquals("String", payloadCol.mappedJavaType());
        assertEquals("@Column(columnDefinition = \"jsonb\")", payloadCol.jpaAnnotation());

        // tags TEXT[] NOT NULL
        ColumnDefinition tagsCol = eventsTable.columns().stream()
                .filter(c -> "tags".equalsIgnoreCase(c.name())).findFirst().orElseThrow();
        assertEquals("List<String>", tagsCol.mappedJavaType());
        assertEquals("@Column\n@JdbcTypeCode(SqlTypes.ARRAY)", tagsCol.jpaAnnotation());

        // amount NUMERIC(10,2) NOT NULL
        ColumnDefinition amountCol = eventsTable.columns().stream()
                .filter(c -> "amount".equalsIgnoreCase(c.name())).findFirst().orElseThrow();
        assertEquals("java.math.BigDecimal", amountCol.mappedJavaType());

        // event_date DATE NOT NULL
        ColumnDefinition dateCol = eventsTable.columns().stream()
                .filter(c -> "event_date".equalsIgnoreCase(c.name())).findFirst().orElseThrow();
        assertEquals("java.time.LocalDate", dateCol.mappedJavaType());
    }

    @Test
    void shouldValidateAndParseSchema4FkExplicit() throws Exception {
        String ddl = """
            CREATE TABLE categories (
                id UUID PRIMARY KEY,
                name VARCHAR(255) NOT NULL
            );
            CREATE TABLE products (
                id UUID PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                category_id UUID,
                CONSTRAINT fk_category FOREIGN KEY (category_id) REFERENCES categories(id)
            );
            """;

        // 1. Execute SQL inside PostgreSQL container
        executeSql(ddl);

        // 2. Parse DDL in-memory
        ParsedSchema parsed = SqlSchemaParser.parse(ddl);

        // 3. Complete structural assertions
        assertNotNull(parsed);
        assertEquals(2, parsed.tables().size());

        TableSchema productsTable = parsed.tables().stream()
                .filter(t -> "products".equalsIgnoreCase(t.name())).findFirst().orElseThrow();

        // Verify column constraint lists
        assertEquals(1, productsTable.constraints().size());
        assertTrue(productsTable.constraints().get(0).contains("fk_category"));

        // Verify target column category_id has its isForeignKey set to true from table-level constraints
        ColumnDefinition catIdCol = productsTable.columns().stream()
                .filter(c -> "category_id".equalsIgnoreCase(c.name())).findFirst().orElseThrow();
        assertTrue(catIdCol.isForeignKey());
        assertEquals("java.util.UUID", catIdCol.mappedJavaType());

        // Verify bi-directional relationship inference from explicit constraints
        assertEquals(2, parsed.relationships().size());

        RelationshipDefinition rel1 = parsed.relationships().stream()
                .filter(r -> "products".equals(r.sourceTable())).findFirst().orElseThrow();
        assertEquals("categories", rel1.targetTable());
        assertEquals("ManyToOne", rel1.relationshipType());

        RelationshipDefinition rel2 = parsed.relationships().stream()
                .filter(r -> "categories".equals(r.sourceTable())).findFirst().orElseThrow();
        assertEquals("products", rel2.targetTable());
        assertEquals("OneToMany", rel2.relationshipType());
    }

    private void executeSql(String sql) throws Exception {
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
