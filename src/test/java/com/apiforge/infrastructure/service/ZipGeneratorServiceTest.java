package com.apiforge.infrastructure.service;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZipGeneratorServiceTest {

    private final ZipGeneratorService service = new ZipGeneratorService();

    @Test
    void testGenerateZip_IntegrityAndStructure() throws IOException {
        // 1. Arrange a mock virtual files map representing a Maven layout
        Map<String, String> files = new HashMap<>();
        files.put("pom.xml", "<project><name>APIForge App</name></project>");
        files.put("docker-compose.yml", "version: '3.8'\nservices:\n  db:\n    image: postgres:15");
        files.put("src/main/java/com/example/demo/Application.java", "package com.example.demo;\npublic class Application {}");
        
        // Mixed OS backslashes to challenge path normalization
        files.put("src\\main\\resources\\application.properties", "spring.datasource.url=jdbc:postgresql://db:5432/apiforge");

        // 2. Act
        byte[] zipBytes = service.generateZip(files);

        // 3. Assert - Read the zip in-memory using ZipInputStream
        assertNotNull(zipBytes);
        assertTrue(zipBytes.length > 0, "ZIP should not be empty");

        Map<String, String> extractedFiles = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String path = entry.getName();
                
                // Assert platform independence constraints
                assertFalse(path.contains("\\"), "ZIP path must not contain backslashes: " + path);
                assertFalse(path.startsWith("/"), "ZIP path must not start with a leading slash: " + path);

                // Read bytes
                ByteArrayOutputStreamBuffer outputBuffer = new ByteArrayOutputStreamBuffer();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    outputBuffer.write(buffer, 0, len);
                }
                
                extractedFiles.put(path, outputBuffer.toString());
                zis.closeEntry();
            }
        }

        // 4. Assert key files exist at correct normalized paths and contain precise content
        assertEquals(4, extractedFiles.size(), "Extracted files count must match input files");
        
        assertTrue(extractedFiles.containsKey("pom.xml"));
        assertEquals("<project><name>APIForge App</name></project>", extractedFiles.get("pom.xml"));

        assertTrue(extractedFiles.containsKey("docker-compose.yml"));
        assertTrue(extractedFiles.get("docker-compose.yml").contains("postgres:15"));

        assertTrue(extractedFiles.containsKey("src/main/java/com/example/demo/Application.java"));

        // Verify Windows backslash path was normalized to standard forward slash
        assertTrue(extractedFiles.containsKey("src/main/resources/application.properties"), 
                "Windows backslash path should be normalized to forward slashes");
        assertEquals("spring.datasource.url=jdbc:postgresql://db:5432/apiforge", 
                extractedFiles.get("src/main/resources/application.properties"));
    }

    @Test
    void testGenerateZip_EdgeCases() {
        // Assert null/empty structures reject gracefully with IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> service.generateZip(null));
        assertThrows(IllegalArgumentException.class, () -> service.generateZip(new HashMap<>()));
    }

    /**
     * Helper subclass of ByteArrayOutputStream to convert content to UTF-8 without throwing exceptions.
     */
    private static class ByteArrayOutputStreamBuffer extends java.io.ByteArrayOutputStream {
        @Override
        public String toString() {
            return new String(buf, 0, count, StandardCharsets.UTF_8);
        }
    }
}
