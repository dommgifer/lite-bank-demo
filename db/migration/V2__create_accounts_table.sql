-- V2: Create accounts table
CREATE TABLE accounts (
    account_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_balance_positive CHECK (balance >= 0),
    CONSTRAINT chk_currency_code CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_status_valid CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'))
);

-- Indexes for performance
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_currency ON accounts(currency);
CREATE INDEX idx_accounts_status ON accounts(status);

-- Insert demo accounts
INSERT INTO accounts (user_id, currency, balance) VALUES
    -- Alice's accounts
    (1, 'USD', 1000.00),
    (1, 'EUR', 500.00),
    (1, 'TWD', 30000.00),
    -- Bob's accounts
    (2, 'USD', 2000.00),
    (2, 'JPY', 100000.00),
    -- Charlie's accounts
    (3, 'USD', 500.00),
    (3, 'EUR', 1000.00);
