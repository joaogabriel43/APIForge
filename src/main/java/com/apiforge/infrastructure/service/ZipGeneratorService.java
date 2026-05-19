package com.apiforge.infrastructure.service;

import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service responsible for packaging generated virtual file systems into structural ZIP artifacts.
 * Operating in pure in-memory buffers to guarantee high throughput and platform-agnostic portability.
 */
@Service
public class ZipGeneratorService {

    /**
     * Packages a map of file paths and contents into a raw zip byte array.
     * Enforces strict forward slashes '/' in all entries to protect UNIX and Windows deployment parity.
     *
     * @param files Map where key is the logical relative path, and value is the source code content.
     * @return Raw compressed zip byte array.
     * @throws IOException If byte stream write operations fail.
     */
    public byte[] generateZip(Map<String, String> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Files map cannot be null or empty");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                String rawPath = entry.getKey();
                if (rawPath == null || rawPath.isBlank()) {
                    continue;
                }

                // Protect against OS backslash differences
                String normalizedPath = rawPath.replace('\\', '/');
                
                // Strip leading slashes to prevent absolute ZIP extraction issues
                if (normalizedPath.startsWith("/")) {
                    normalizedPath = normalizedPath.substring(1);
                }

                ZipEntry zipEntry = new ZipEntry(normalizedPath);
                zos.putNextEntry(zipEntry);

                String content = entry.getValue() != null ? entry.getValue() : "";
                byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
                zos.write(contentBytes);
                
                zos.closeEntry();
            }
            zos.finish();
        }
        return baos.toByteArray();
    }
}
