-- V4: Create saga_executions table for SAGA pattern tracking
CREATE TABLE saga_executions (
    saga_id VARCHAR(100) PRIMARY KEY,
    transaction_id BIGINT,
    current_step INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    compensate_from_step INTEGER,
    error_message TEXT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saga_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE SET NULL,
    CONSTRAINT chk_saga_status CHECK (status IN (
        'PENDING', 'IN_PROGRESS', 'COMPLETED', 'COMPENSATING', 'COMPENSATED', 'FAILED'
    )),
    CONSTRAINT chk_current_step CHECK (current_step >= 0 AND current_step <= 10)
);

-- Indexes for SAGA tracking
CREATE INDEX idx_saga_transaction_id ON saga_executions(transaction_id);
CREATE INDEX idx_saga_status ON saga_executions(status);
CREATE INDEX idx_saga_created_at ON saga_executions(created_at DESC);
CREATE INDEX idx_saga_updated_at ON saga_executions(updated_at DESC);
