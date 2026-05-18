package com.apiforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * APIForge Application Entrypoint.
 * Bootstraps the Spring Boot Application and handles component scanning.
 */
@SpringBootApplication
public class ApiForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiForgeApplication.class, args);
    }
}
