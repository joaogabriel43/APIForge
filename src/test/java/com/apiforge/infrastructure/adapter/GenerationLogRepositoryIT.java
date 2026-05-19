package com.apiforge.infrastructure.adapter;

import com.apiforge.domain.model.GenerationLog;
import com.apiforge.domain.model.GenerationOptions;
import com.apiforge.domain.repository.GenerationLogRepository;
import com.apiforge.infrastructure.db.entity.GenerationLogEntity;
import com.apiforge.infrastructure.db.repository.JpaGenerationLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration Test for verifying that Flyway applies migration V2 correctly on the active real database,
 * and that Hibernate persists and deserializes our JSONB columns properly.
 * Employs JUnit 5 Assumptions to skip execution gracefully if the local Postgres instance is offline
 * or credentials fail authentication.
 */
@SpringBootTest
class GenerationLogRepositoryIT {

    @Autowired
    private GenerationLogRepository repository;

    @Autowired
    private JpaGenerationLogRepository jpaRepository;

    @Autowired
    private DataSource dataSource;

    private UUID createdLogId;

    @BeforeEach
    void verifyDatabaseConnectivity() {
        assumeTrue(isDatabaseOnline(), "Skipping database integration tests because the local PostgreSQL instance is unreachable or credentials failed authentication.");
    }

    @AfterEach
    void tearDown() {
        if (createdLogId != null) {
            try {
                // Keep the database pristine by cleaning up our test records
                jpaRepository.deleteById(createdLogId);
            } catch (Exception e) {
                // Ignore cleanup failures
            }
        }
    }

    @Test
    void shouldSuccessfullyApplyFlywayMigrationAndPersistLogWithJsonbOptions() {
        createdLogId = UUID.randomUUID();
        String sqlHash = "8b2fd1ca9ca0e73cfe018cf8ffcb661e88f75da1d2353214d50def02b8b343c2";
        String packageName = "com.apiforge.generated";
        GenerationOptions options = new GenerationOptions(packageName, true, false, true);
        int fileCount = 12;
        OffsetDateTime createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

        GenerationLog domainLog = new GenerationLog(
                createdLogId,
                sqlHash,
                packageName,
                options,
                fileCount,
                createdAt
        );

        // 1. Persist the log using the secondary adapter (GenerationLogRepositoryImpl)
        assertDoesNotThrow(() -> repository.save(domainLog));

        // 2. Fetch the entity directly via standard JpaRepository to inspect persistence details
        Optional<GenerationLogEntity> fetchedOpt = jpaRepository.findById(createdLogId);
        assertTrue(fetchedOpt.isPresent(), "Persisted log should be found in database");

        GenerationLogEntity entity = fetchedOpt.get();
        assertEquals(createdLogId, entity.getId());
        assertEquals(sqlHash, entity.getSqlHash());
        assertEquals(packageName, entity.getPackageName());
        assertEquals(fileCount, entity.getFileCount());

        // Assert equality of truncated createdAt timestamp
        assertEquals(createdAt.toInstant(), entity.getCreatedAt().toInstant());

        // 3. Critically assert that the JSONB column was deserialized correctly back into a GenerationOptions record
        assertNotNull(entity.getOptions());
        assertEquals(packageName, entity.getOptions().packageName());
        assertTrue(entity.getOptions().generateJwt());
        assertFalse(entity.getOptions().generatePagination());
        assertTrue(entity.getOptions().generateSoftDelete());
    }

    private boolean isDatabaseOnline() {
        try (Connection ignored = dataSource.getConnection()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
