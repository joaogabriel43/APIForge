-- ==============================================================================
-- Flyway Database Migration V2: Create generation_log table
-- ==============================================================================

CREATE TABLE generation_log (
    id UUID PRIMARY KEY,
    sql_hash VARCHAR(64) NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    options JSONB NOT NULL,
    file_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);
