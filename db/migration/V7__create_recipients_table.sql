-- Create recipients table for saved recipient management
-- This table stores the list of frequently used recipients for each user

CREATE TABLE IF NOT EXISTS recipients
(
    recipient_id   BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    account_number VARCHAR(20)  NOT NULL,
    nickname       VARCHAR(100),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint
    CONSTRAINT fk_recipient_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id)
            ON DELETE CASCADE,

    -- Unique constraint: one user cannot save the same account number twice
    CONSTRAINT uk_user_account
        UNIQUE (user_id, account_number)
);

-- Create index for faster lookups
CREATE INDEX idx_recipients_user_id ON recipients (user_id);

-- Insert some demo data for testing
-- Alice (user_id=1) saves Bob's and Charlie's accounts
-- Bob (user_id=2) saves Alice's account
INSERT INTO recipients (user_id, account_number, nickname)
VALUES (1, '001-03-0000005', 'Bob - JPY Account'),
       (1, '001-03-0000006', 'Charlie - USD Account'),
       (2, '001-03-0000002', 'Alice - EUR Account')
ON CONFLICT (user_id, account_number) DO NOTHING;
