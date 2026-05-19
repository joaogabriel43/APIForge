package com.apiforge.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Configuration to enable asynchronous execution via {@link EnableAsync}.
 * Required for our fire-and-forget generation audit logging mechanism.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
