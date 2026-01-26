-- V8: Create analytics tables for financial health indicators
-- These tables store pre-aggregated data for the analytics-query-service

-- Daily summary table: stores daily aggregated transaction data per user/currency
CREATE TABLE daily_summary (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    summary_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total_income DECIMAL(18,2) DEFAULT 0,
    total_expense DECIMAL(18,2) DEFAULT 0,
    transaction_count INT DEFAULT 0,
    ending_balance DECIMAL(18,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_daily_summary_user_date_currency UNIQUE(user_id, summary_date, currency)
);

-- Monthly summary table: stores monthly aggregated data per user/currency
CREATE TABLE monthly_summary (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    year_month VARCHAR(7) NOT NULL,  -- Format: '2024-01'
    currency VARCHAR(3) NOT NULL,
    total_income DECIMAL(18,2) DEFAULT 0,
    total_expense DECIMAL(18,2) DEFAULT 0,
    net_change DECIMAL(18,2) DEFAULT 0,
    savings_rate DECIMAL(5,2),  -- Percentage: (income - expense) / income * 100
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_monthly_summary_user_month_currency UNIQUE(user_id, year_month, currency)
);

-- Balance snapshot table: stores daily balance snapshots for trend analysis
CREATE TABLE balance_snapshot (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance DECIMAL(18,2) NOT NULL,
    snapshot_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_balance_snapshot_account_date UNIQUE(account_id, snapshot_date)
);

-- Indexes for query performance
CREATE INDEX idx_daily_summary_user_date ON daily_summary(user_id, summary_date);
CREATE INDEX idx_daily_summary_user_date_range ON daily_summary(user_id, summary_date DESC);
CREATE INDEX idx_monthly_summary_user_month ON monthly_summary(user_id, year_month);
CREATE INDEX idx_monthly_summary_user_month_range ON monthly_summary(user_id, year_month DESC);
CREATE INDEX idx_balance_snapshot_user_date ON balance_snapshot(user_id, snapshot_date);
CREATE INDEX idx_balance_snapshot_user_date_range ON balance_snapshot(user_id, snapshot_date DESC);
CREATE INDEX idx_balance_snapshot_account_date_range ON balance_snapshot(account_id, snapshot_date DESC);

-- Comments for documentation
COMMENT ON TABLE daily_summary IS 'Pre-aggregated daily transaction summaries for analytics';
COMMENT ON TABLE monthly_summary IS 'Pre-aggregated monthly transaction summaries for analytics';
COMMENT ON TABLE balance_snapshot IS 'Daily balance snapshots for trend visualization';
