package com.apiforge.application.service;

import com.apiforge.domain.model.GenerationLog;
import com.apiforge.domain.model.GenerationOptions;
import com.apiforge.domain.repository.GenerationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GenerationAuditServiceTest {

    private GenerationLogRepository repositoryMock;
    private GenerationAuditService auditService;

    @BeforeEach
    void setUp() {
        repositoryMock = mock(GenerationLogRepository.class);
        auditService = new GenerationAuditService(repositoryMock);
    }

    @Test
    void shouldSuccessfullyComputeSha256() {
        String sql = "CREATE TABLE users (id UUID PRIMARY KEY);";
        // Expected SHA-256 for the string above is "8b2fd1ca9ca0e73cfe018cf8ffcb661e88f75da1d2353214d50def02b8b343c2"
        String expectedHash = "8b2fd1ca9ca0e73cfe018cf8ffcb661e88f75da1d2353214d50def02b8b343c2";

        String actualHash = auditService.calculateSha256(sql);
        assertEquals(expectedHash, actualHash);
    }

    @Test
    void shouldHandleNullInputForSha256() {
        String actualHash = auditService.calculateSha256(null);
        // Expected SHA-256 of empty string ""
        String expectedEmptyHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        assertEquals(expectedEmptyHash, actualHash);
    }

    @Test
    void shouldAuditSuccessfullyAndSaveToRepository() {
        String sql = "CREATE TABLE users (id UUID PRIMARY KEY);";
        String expectedHash = "8b2fd1ca9ca0e73cfe018cf8ffcb661e88f75da1d2353214d50def02b8b343c2";
        
        GenerationOptions options = new GenerationOptions("com.example.app", true, false, true);
        int fileCount = 8;

        auditService.audit(sql, options, fileCount);

        ArgumentCaptor<GenerationLog> captor = ArgumentCaptor.forClass(GenerationLog.class);
        verify(repositoryMock, times(1)).save(captor.capture());

        GenerationLog logged = captor.getValue();
        assertNotNull(logged);
        assertNotNull(logged.id());
        assertEquals(expectedHash, logged.sqlHash());
        assertEquals("com.example.app", logged.packageName());
        assertEquals(options, logged.options());
        assertEquals(fileCount, logged.fileCount());
        assertNotNull(logged.createdAt());
    }

    @Test
    void shouldIsolateInternalRepositoryErrorsAndNeverPropagateToCaller() {
        String sql = "CREATE TABLE users (id UUID PRIMARY KEY);";
        GenerationOptions options = new GenerationOptions("com.example.app", true, false, true);

        // Configure repository to throw a database connectivity exception
        doThrow(new RuntimeException("Database is offline! connection timeout."))
                .when(repositoryMock).save(any(GenerationLog.class));

        // Attempting to audit should NOT throw any exceptions, proving absolute fault isolation
        assertDoesNotThrow(() -> auditService.audit(sql, options, 10));

        // Verify save was indeed called once before the error was suppressed
        verify(repositoryMock, times(1)).save(any(GenerationLog.class));
    }
}
