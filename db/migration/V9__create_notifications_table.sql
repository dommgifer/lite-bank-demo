-- V9: Create notifications table for real-time notification service
-- Stores user notifications with SSE push support and REST API queries

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,                    -- 收件人
    type VARCHAR(50) NOT NULL,                  -- 通知類型
    title VARCHAR(200) NOT NULL,                -- 標題（前端顯示）
    message TEXT NOT NULL,                      -- 內容
    metadata JSONB,                             -- 額外資料（金額、對方帳號等）
    read BOOLEAN DEFAULT FALSE,                 -- 已讀狀態
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,                          -- 標記已讀的時間

    CONSTRAINT chk_notification_type CHECK (type IN (
        'TRANSFER_SENT',        -- 你轉出了錢
        'TRANSFER_RECEIVED',    -- 你收到了錢
        'TRANSFER_FAILED',      -- 轉帳失敗
        'DEPOSIT_SUCCESS',      -- 存款成功
        'WITHDRAWAL_SUCCESS',   -- 提款成功
        'EXCHANGE_SUCCESS',     -- 換匯成功
        'SYSTEM'                -- 系統通知
    ))
);

-- 部分索引：只索引未讀通知，提升查詢效能
CREATE INDEX idx_notifications_user_unread
    ON notifications(user_id, created_at DESC)
    WHERE read = FALSE;

-- 一般查詢索引：用於列表顯示
CREATE INDEX idx_notifications_user_created
    ON notifications(user_id, created_at DESC);

-- SSE Last-Event-ID 補取用索引
CREATE INDEX idx_notifications_user_after_id
    ON notifications(user_id, id);

-- 清理任務用索引
CREATE INDEX idx_notifications_cleanup
    ON notifications(read, created_at);

COMMENT ON TABLE notifications IS 'User notifications for real-time SSE push and REST API queries';
COMMENT ON COLUMN notifications.user_id IS '收件人 user ID';
COMMENT ON COLUMN notifications.type IS '通知類型：TRANSFER_SENT, TRANSFER_RECEIVED, etc.';
COMMENT ON COLUMN notifications.metadata IS 'JSONB 格式的額外資料，如金額、對方帳號等';
COMMENT ON COLUMN notifications.read IS '是否已讀';
COMMENT ON COLUMN notifications.read_at IS '標記已讀的時間戳';
