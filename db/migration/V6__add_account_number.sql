-- Add account_number column with structured format: {branch 3}-{type 2}-{sequence 7}
-- Format: XXX-YY-ZZZZZZZ
-- Branch: 001 = Head Office
-- Type: 01 = Savings (TWD), 03 = Foreign Currency (USD, EUR, JPY, etc.)
-- Sequence: 7-digit zero-padded number

ALTER TABLE accounts
ADD COLUMN account_number VARCHAR(15);

-- Generate account numbers for existing accounts
-- Using branch 001, type based on currency, and account_id as sequence
UPDATE accounts
SET account_number = CONCAT(
    '001-',
    CASE
        WHEN currency = 'TWD' THEN '01'
        ELSE '03'
    END,
    '-',
    LPAD(account_id::TEXT, 7, '0')
);

-- Make account_number NOT NULL and UNIQUE after populating
ALTER TABLE accounts
ALTER COLUMN account_number SET NOT NULL;

ALTER TABLE accounts
ADD CONSTRAINT unique_account_number UNIQUE (account_number);

-- Add index for account_number lookups
CREATE INDEX idx_accounts_account_number ON accounts(account_number);

COMMENT ON COLUMN accounts.account_number IS 'Structured account number: {branch 3}-{type 2}-{sequence 7}';
