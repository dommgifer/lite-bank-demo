-- Database initialization script
-- This runs before Flyway migrations

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set timezone
SET timezone = 'UTC';

-- Create database if not exists (this file runs in docker-entrypoint-initdb.d)
-- The POSTGRES_DB environment variable already creates the database

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE litebank TO litebank_user;
