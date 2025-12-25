-- V3: Create transactions table (append-only)
CREATE TABLE transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    account_id INTEGER NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance_after NUMERIC(15, 2) NOT NULL,
    reference_id VARCHAR(100),
    description TEXT,
    trace_id VARCHAR(32),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    CONSTRAINT chk_transaction_type CHECK (transaction_type IN (
        'DEPOSIT', 'WITHDRAWAL', 'TRANSFER_OUT', 'TRANSFER_IN',
        'EXCHANGE_OUT', 'EXCHANGE_IN', 'FEE'
    )),
    CONSTRAINT chk_balance_after_positive CHECK (balance_after >= 0)
);

-- Indexes for query performance
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);
CREATE INDEX idx_transactions_trace_id ON transactions(trace_id);
CREATE INDEX idx_transactions_reference_id ON transactions(reference_id);
CREATE INDEX idx_transactions_type ON transactions(transaction_type);

-- Composite index for common query pattern
CREATE INDEX idx_transactions_account_date ON transactions(account_id, created_at DESC);
