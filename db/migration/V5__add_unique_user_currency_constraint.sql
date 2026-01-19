-- Add unique constraint to prevent duplicate accounts for same user and currency
-- Business rule: Each user can only have one account per currency

ALTER TABLE accounts
ADD CONSTRAINT unique_user_currency UNIQUE (user_id, currency);

COMMENT ON CONSTRAINT unique_user_currency ON accounts IS 'Ensures each user can only have one account per currency';
