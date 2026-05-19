package com.apiforge.application.service;

import com.apiforge.domain.model.GenerationLog;
import com.apiforge.domain.model.GenerationOptions;
import com.apiforge.domain.repository.GenerationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Application service managing generation audit logs.
 * Runs operations asynchronously using standard Spring threads.
 */
@Service
public class GenerationAuditService {

    private static final Logger log = LoggerFactory.getLogger(GenerationAuditService.class);
    private final GenerationLogRepository repository;

    /**
     * Constructor injection for domain repository port.
     * 
     * @param repository The boundary persistence port.
     */
    public GenerationAuditService(GenerationLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Performs an asynchronous, fire-and-forget audit entry persisting.
     * Guarantees that any exception thrown during hash calculation or database persistence
     * is isolated internally and never bubbles up to impact the final REST response.
     * 
     * @param sql        The original raw SQL input string.
     * @param options    The generation parameters.
     * @param fileCount  The count of files successfully compiled.
     */
    @Async
    public void audit(String sql, GenerationOptions options, int fileCount) {
        try {
            String hash = calculateSha256(sql);
            GenerationLog logEntry = new GenerationLog(
                UUID.randomUUID(),
                hash,
                options.packageName(),
                options,
                fileCount,
                OffsetDateTime.now()
            );
            
            repository.save(logEntry);
            log.info("Successfully persisted generation audit log. Package: {}, SQL Hash: {}", 
                     options.packageName(), hash);
        } catch (Exception e) {
            // Strictly catch all errors (banco fora, mapping issues, null pointer exceptions)
            // to fulfill the client-isolation resilience criteria
            log.error("Internal APIForge persistent logging failure. Audit operation failed but will not propagate to client response.", e);
        }
    }

    /**
     * Calculates the cryptographic SHA-256 hash of a string.
     * 
     * @param input Raw text.
     * @return Cryptographic Hex representation of the computed SHA-256 hash.
     */
    public String calculateSha256(String input) {
        if (input == null) {
            input = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("SHA-256 digest computation failed. Falling back to blank hash.", e);
            return "";
        }
    }
}
